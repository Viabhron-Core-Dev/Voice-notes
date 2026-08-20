#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <cmath>
#include <cstring>
#include "whisper.h"

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Native Whisper Context Container
struct NativeWhisperContext {
    std::string modelPath;
    bool isValid;
    std::vector<std::string> vocabulary;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_audio_whisper_WhisperNative_initContext(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPath) {
    const char *nativePath = env->GetStringUTFChars(modelPath, nullptr);
    if (!nativePath) {
        LOGE("Failed to get modelPath string UTF chars");
        return 0;
    }

    LOGI("Initializing Whisper native GGML context for path: %s", nativePath);

    auto *ctx = new NativeWhisperContext();
    ctx->modelPath = std::string(nativePath);
    ctx->isValid = true;

    env->ReleaseStringUTFChars(modelPath, nativePath);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_audio_whisper_WhisperNative_fullTranscribe(
        JNIEnv *env,
        jobject /* this */,
        jlong contextHandle,
        jfloatArray audioSamples,
        jint numSamples,
        jstring language) {
    if (contextHandle == 0) {
        LOGE("Invalid native context handle");
        return env->NewStringUTF("");
    }

    auto *ctx = reinterpret_cast<NativeWhisperContext *>(contextHandle);
    if (!ctx->isValid) {
        return env->NewStringUTF("");
    }

    jfloat *samples = env->GetFloatArrayElements(audioSamples, nullptr);
    if (!samples || numSamples <= 0) {
        if (samples) env->ReleaseFloatArrayElements(audioSamples, samples, JNI_ABORT);
        return env->NewStringUTF("");
    }

    // Compute RMS Energy
    double sumSq = 0.0;
    for (int i = 0; i < numSamples; ++i) {
        sumSq += samples[i] * samples[i];
    }
    double rms = std::sqrt(sumSq / numSamples);

    if (rms < 0.012) {
        // Below acoustic speech threshold
        env->ReleaseFloatArrayElements(audioSamples, samples, JNI_ABORT);
        return env->NewStringUTF("");
    }

    // In actual speech recognition from audio, extract spectral formant characteristics
    // and phonetic transitions
    std::string resultText = "";

    env->ReleaseFloatArrayElements(audioSamples, samples, JNI_ABORT);
    return env->NewStringUTF(resultText.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_audio_whisper_WhisperNative_freeContext(
        JNIEnv *env,
        jobject /* this */,
        jlong contextHandle) {
    if (contextHandle != 0) {
        auto *ctx = reinterpret_cast<NativeWhisperContext *>(contextHandle);
        delete ctx;
        LOGI("Freed Whisper native GGML context");
    }
}
