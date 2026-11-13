import Foundation
import llama

enum EngineError: Error {
    case missingModel
    case loadFailed
    case notLoaded
    case contextFull
    case decodeFailed
}

/// Orbit 412's voice: the bundled language model, run on the phone with llama.cpp (Metal on a
/// device, CPU in the simulator). One conversation is kept in the context at a time; when the
/// next prompt starts with what is already there, only the new part is read again.
actor LlamaEngine {
    static let shared = LlamaEngine()

    /// Context window, as on Android (about 0.8 GB while chatting).
    static let contextSize: UInt32 = 2048
    /// Longest answer, in tokens.
    static let maxNewTokens = 384

    private var model: OpaquePointer?
    private var context: OpaquePointer?
    private var vocab: OpaquePointer?
    private var sampler: UnsafeMutablePointer<llama_sampler>?
    private var template: String?
    /// The tokens now held in the context (sequence 0).
    private var cached: [llama_token] = []

    var isLoaded: Bool { context != nil }

    static var modelPath: String? { Bundle.main.path(forResource: "hw-core", ofType: "gguf") }

    func load() throws {
        if context != nil { return }
        guard let path = Self.modelPath else { throw EngineError.missingModel }
        llama_backend_init()
        var modelParams = llama_model_default_params()
        #if targetEnvironment(simulator)
        modelParams.n_gpu_layers = 0
        #else
        modelParams.n_gpu_layers = 99
        #endif
        guard let m = llama_model_load_from_file(path, modelParams) else { throw EngineError.loadFailed }
        var params = llama_context_default_params()
        params.n_ctx = Self.contextSize
        params.n_batch = 512
        params.n_ubatch = 512
        let threads = Int32(max(2, min(6, ProcessInfo.processInfo.activeProcessorCount - 2)))
        params.n_threads = threads
        params.n_threads_batch = threads
        guard let c = llama_init_from_model(m, params) else {
            llama_model_free(m)
            throw EngineError.loadFailed
        }
        model = m
        context = c
        vocab = llama_model_get_vocab(m)
        if let t = llama_model_chat_template(m, nil) { template = String(cString: t) }

        // The Android app's sampling for this model: calm, focused, a little variety.
        let chain = llama_sampler_chain_init(llama_sampler_chain_default_params())
        llama_sampler_chain_add(chain, llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 64, 1.1, 0, 0))
        llama_sampler_chain_add(chain, llama_sampler_init_top_k(20))
        llama_sampler_chain_add(chain, llama_sampler_init_top_p(0.8, 1))
        llama_sampler_chain_add(chain, llama_sampler_init_temp(0.7))
        llama_sampler_chain_add(chain, llama_sampler_init_dist(UInt32.random(in: 1...UInt32.max)))
        sampler = chain
    }

    /// Answers [prompt] after [system] and the earlier turns in [history] (oldest first). Long
    /// histories are trimmed to the newest turns that fit. [onPartial] receives the raw text so
    /// far, every few tokens. Cancelling the calling task ends the answer with what arrived.
    func answer(
        system: String,
        history: [(role: String, content: String)],
        prompt: String,
        onPartial: @escaping @Sendable (String) -> Void
    ) throws -> String {
        for budget in [700, 350, 0] {
            let turns = Self.trim(history, budgetTokens: budget)
            let messages = [(role: "system", content: system)] + turns + [(role: "user", content: prompt)]
            do {
                return try generate(messages, onPartial: onPartial)
            } catch EngineError.contextFull {
                continue
            }
        }
        throw EngineError.contextFull
    }

    /// Newest turns that fit in about [budgetTokens] tokens (4 characters per token), oldest
    /// first, starting with a question.
    static func trim(_ all: [(role: String, content: String)], budgetTokens: Int) -> [(role: String, content: String)] {
        var used = 0
        var kept: [(role: String, content: String)] = []
        for turn in all.reversed() {
            used += turn.content.count / 4 + 8
            if used > budgetTokens { break }
            kept.insert(turn, at: 0)
        }
        while let first = kept.first, first.role != "user" { kept.removeFirst() }
        return kept
    }

    private func generate(_ messages: [(role: String, content: String)], onPartial: @escaping @Sendable (String) -> Void) throws -> String {
        guard let context, let vocab, let sampler else { throw EngineError.notLoaded }
        let prompt = format(messages)
        let tokens = tokenize(prompt)
        guard !tokens.isEmpty else { throw EngineError.decodeFailed }
        if tokens.count + Self.maxNewTokens > Int(llama_n_ctx(context)) { throw EngineError.contextFull }

        // Keep what the context already holds of this prompt; read only the rest.
        var common = 0
        while common < cached.count, common < tokens.count, cached[common] == tokens[common] { common += 1 }
        if common == tokens.count { common -= 1 } // the last token is read again to get its logits
        let memory = llama_get_memory(context)
        if !llama_memory_seq_rm(memory, 0, Int32(common), -1) {
            llama_memory_clear(memory, true)
            common = 0
        }
        cached = Array(cached.prefix(common))

        let batchSize = Int(llama_n_batch(context))
        var index = common
        while index < tokens.count {
            let end = min(index + batchSize, tokens.count)
            var chunk = Array(tokens[index..<end])
            let rc = chunk.withUnsafeMutableBufferPointer { buffer in
                llama_decode(context, llama_batch_get_one(buffer.baseAddress, Int32(buffer.count)))
            }
            if rc != 0 {
                llama_memory_clear(memory, true)
                cached = []
                throw rc == 1 ? EngineError.contextFull : EngineError.decodeFailed
            }
            cached.append(contentsOf: chunk)
            index = end
        }

        llama_sampler_reset(sampler)
        var bytes: [UInt8] = []
        var text = ""
        var sinceEmit = 0
        for _ in 0..<Self.maxNewTokens {
            if Task.isCancelled { break }
            let token = llama_sampler_sample(sampler, context, -1)
            if llama_vocab_is_eog(vocab, token) { break }
            bytes += piece(token)
            if let s = String(bytes: bytes, encoding: .utf8) {
                text += s
                bytes.removeAll()
            }
            var next = token
            let rc = withUnsafeMutablePointer(to: &next) { llama_decode(context, llama_batch_get_one($0, 1)) }
            if rc != 0 { break }
            cached.append(token)
            if text.suffix(24).contains("<|im_end|>") || text.suffix(24).contains("<|im_start|>") { break }
            sinceEmit += 1
            if sinceEmit >= 3 {
                sinceEmit = 0
                onPartial(text)
            }
        }
        return text
    }

    /// The conversation in the model's own chat format, ending where the answer starts.
    private func format(_ messages: [(role: String, content: String)]) -> String {
        let roles = messages.map { strdup($0.role) }
        let contents = messages.map { strdup($0.content) }
        defer {
            roles.forEach { free($0) }
            contents.forEach { free($0) }
        }
        var chat = zip(roles, contents).map { llama_chat_message(role: UnsafePointer($0), content: UnsafePointer($1)) }
        let count = chat.count
        let total = messages.reduce(0) { $0 + $1.content.utf8.count + $1.role.utf8.count }
        var capacity = max(4096, total * 2 + 512)
        var buffer = [CChar](repeating: 0, count: capacity)
        var n = llama_chat_apply_template(template, &chat, count, true, &buffer, Int32(capacity))
        if n > Int32(capacity) {
            capacity = Int(n) + 1
            buffer = [CChar](repeating: 0, count: capacity)
            n = llama_chat_apply_template(template, &chat, count, true, &buffer, Int32(capacity))
        }
        if n > 0 {
            return String(decoding: buffer.prefix(Int(n)).map { UInt8(bitPattern: $0) }, as: UTF8.self)
        }
        // ChatML, the format this model was trained on.
        return messages.map { "<|im_start|>\($0.role)\n\($0.content)<|im_end|>\n" }.joined() + "<|im_start|>assistant\n"
    }

    private func tokenize(_ text: String) -> [llama_token] {
        let length = Int32(text.utf8.count)
        var capacity = Int32(length) + 16
        var tokens = [llama_token](repeating: 0, count: Int(capacity))
        var n = llama_tokenize(vocab, text, length, &tokens, capacity, true, true)
        if n < 0 {
            capacity = -n
            tokens = [llama_token](repeating: 0, count: Int(capacity))
            n = llama_tokenize(vocab, text, length, &tokens, capacity, true, true)
        }
        return Array(tokens.prefix(Int(max(0, n))))
    }

    private func piece(_ token: llama_token) -> [UInt8] {
        var capacity: Int32 = 64
        var buffer = [CChar](repeating: 0, count: Int(capacity))
        var n = llama_token_to_piece(vocab, token, &buffer, capacity, 0, false)
        if n < 0 {
            capacity = -n
            buffer = [CChar](repeating: 0, count: Int(capacity))
            n = llama_token_to_piece(vocab, token, &buffer, capacity, 0, false)
        }
        return buffer.prefix(Int(max(0, n))).map { UInt8(bitPattern: $0) }
    }
}
