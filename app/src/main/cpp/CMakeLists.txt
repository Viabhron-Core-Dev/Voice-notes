cmake_minimum_required(VERSION 3.22.1)

project("whisper-jni")

add_library(
        whisper-jni
        SHARED
        whisper_jni.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        whisper-jni
        ${log-lib}
        android
)
