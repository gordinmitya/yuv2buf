cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_SOURCE_DIR}/cpp/includes)

add_library(MNN SHARED IMPORTED)
set_target_properties(
        MNN
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jni/${ANDROID_ABI}/libMNN.so
)

file(GLOB_RECURSE CPP_SRCS
        ${CMAKE_SOURCE_DIR}/cpp/mnnnetnative.cpp
        ${CMAKE_SOURCE_DIR}/cpp/mnn_helper_native.cpp)
add_library(mnncore SHARED ${CPP_SRCS})

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

add_definitions(-DMNN_USE_LOGCAT)
target_link_libraries(mnncore MNN ${log-lib} ${jnigraphics-lib})
