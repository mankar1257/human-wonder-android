import AVFoundation
import Observation
import Speech
import UIKit

/// Soft taps: one when a question goes up, one when an answer lands.
@MainActor
enum Haptics {
    static func tap() { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
    static func landed() { UINotificationFeedbackGenerator().notificationOccurred(.success) }
}

/// "Read answers aloud": Orbit 412 speaks each answer with the phone's own voice.
@MainActor
final class Speaker {
    private let synthesizer = AVSpeechSynthesizer()

    func speak(_ text: String) {
        stop()
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: AVSpeechSynthesisVoice.currentLanguageCode())
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.96
        synthesizer.speak(utterance)
    }

    func stop() {
        if synthesizer.isSpeaking { synthesizer.stopSpeaking(at: .immediate) }
    }
}

/// The mic in the composer: speech becomes the question, recognised on the phone only.
@MainActor
@Observable
final class VoiceInput {
    var listening = false
    /// Set when this phone can't turn speech into text on the phone itself.
    var unavailable = false

    private let audio = AVAudioEngine()
    @ObservationIgnored private var request: SFSpeechAudioBufferRecognitionRequest?
    @ObservationIgnored private var task: SFSpeechRecognitionTask?
    @ObservationIgnored private var tapInstalled = false

    func toggle(onText: @escaping (String) -> Void) {
        if listening {
            stop()
        } else {
            Task { await start(onText: onText) }
        }
    }

    private func start(onText: @escaping (String) -> Void) async {
        let speechAllowed = await withCheckedContinuation { (c: CheckedContinuation<Bool, Never>) in
            SFSpeechRecognizer.requestAuthorization { c.resume(returning: $0 == .authorized) }
        }
        let micAllowed = await AVAudioApplication.requestRecordPermission()
        guard speechAllowed, micAllowed,
              let recognizer = SFSpeechRecognizer(), recognizer.isAvailable, recognizer.supportsOnDeviceRecognition
        else {
            unavailable = true
            return
        }
        // A second tap while the permission prompts were open must not start a second recording.
        guard !listening, !tapInstalled else { return }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: [.duckOthers])
            try session.setActive(true, options: .notifyOthersOnDeactivation)
            let request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            request.requiresOnDeviceRecognition = true
            self.request = request
            let input = audio.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else { // no microphone
                try? session.setActive(false, options: .notifyOthersOnDeactivation)
                unavailable = true
                return
            }
            input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
                request.append(buffer)
            }
            tapInstalled = true
            audio.prepare()
            try audio.start()
            listening = true
            task = recognizer.recognitionTask(with: request) { [weak self] result, error in
                let text = result?.bestTranscription.formattedString
                let done = (result?.isFinal ?? false) || error != nil
                Task { @MainActor in
                    if let text, !text.isEmpty { onText(text) }
                    if done { self?.stop() }
                }
            }
        } catch {
            stop()
            unavailable = true
        }
    }

    func stop() {
        guard listening || tapInstalled else { return }
        audio.stop()
        if tapInstalled {
            audio.inputNode.removeTap(onBus: 0)
            tapInstalled = false
        }
        request?.endAudio()
        task?.finish()
        request = nil
        task = nil
        listening = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}
