cmake_minimum_required(VERSION 3.22.1)
project("smollm")

# Native glue for Human Wonder, adapted from OfflineLLM's smollm module (Apache-2.0, jegly),
# which adapted it from SmolChat-Android (Apache-2.0, shubham0204).
# Human Wonder change: CPU only. The Vulkan backend, its shader generator (which needs a host
# C/C++ compiler) and the Khronos headers are gone; a 0.6B model is fast enough on the CPU.

# llama.cpp is the git submodule at third_party/llama.cpp, pinned to the same commit OfflineLLM
# uses (620d2225cc436ab0b001e3200e590e3b286ef9ab). tools/build-on-mac.sh fetches it if missing.
set(LLAMA_DIR_RELATIVE "../../../../third_party/llama.cpp")
get_filename_component(LLAMA_DIR ${LLAMA_DIR_RELATIVE} ABSOLUTE)
if(NOT EXISTS "${LLAMA_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "llama.cpp is missing at ${LLAMA_DIR}. Run tools/build-on-mac.sh or "
                        "git submodule update --init third_party/llama.cpp")
endif()
add_subdirectory(${LLAMA_DIR} llama.cpp)

set(GGML_DIR ${LLAMA_DIR}/ggml)

target_compile_options(
        llama
        PUBLIC
        -fvisibility=hidden -fvisibility-inlines-hidden
)
target_compile_options(
        llama
        PUBLIC
        -ffunction-sections -fdata-sections
)
target_link_options(
        llama
        PRIVATE
        -Wl,--gc-sections -flto
        -Wl,--exclude-libs,ALL
)

# Single JNI wrapper library. CPU-specific optimisation lives in the ggml-cpu
# plugin variants (GGML_CPU_ALL_VARIANTS) selected at runtime, not here — the
# wrapper is thin glue and arch flags on it never affected inference speed.
add_library(
        smollm
        SHARED
        LLMInference.cpp
        smollm.cpp
)
target_include_directories(
        smollm
        PUBLIC
        ${GGML_DIR}/include
        ${GGML_DIR}/src
        ${LLAMA_DIR}/include
)
target_compile_options(
        smollm
        PUBLIC
        -O3
        -fvisibility=hidden -fvisibility-inlines-hidden
        -ffunction-sections -fdata-sections
)
# Note: deliberately NOT linking llama-common — it contains upstream's
# HTTP/download machinery, which has no place in a zero-network app. The few
# helpers the wrapper needed are reimplemented locally on the llama.h C API.
target_link_libraries(
        smollm
        android log llama
)
target_link_options(
        smollm
        PRIVATE
        -Wl,--gc-sections -flto
        -Wl,--exclude-libs,ALL
)

# GGUFReader library
set(TARGET_NAME_GGUF_READER ggufreader)
add_library(${TARGET_NAME_GGUF_READER} SHARED GGUFReader.cpp)
target_include_directories(
        ${TARGET_NAME_GGUF_READER}
        PUBLIC
        ${GGML_DIR}/include
)
target_compile_options(
        ${TARGET_NAME_GGUF_READER}
        PUBLIC
        -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections
)
target_link_libraries(
        ${TARGET_NAME_GGUF_READER}
        ggml
)
target_link_options(
        ${TARGET_NAME_GGUF_READER}
        PRIVATE
        -Wl,--gc-sections -flto
        -Wl,--exclude-libs,ALL
)
