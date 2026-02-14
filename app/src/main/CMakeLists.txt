cmake_minimum_required(VERSION 3.13)
project(vosk_jni)
set(CMAKE_ANDROID_PAGE_SIZE 16384)


add_library(vosk_jni SHARED vosk_jni.cc)

find_library(log-lib log)

target_link_libraries(vosk_jni ${log-lib})
