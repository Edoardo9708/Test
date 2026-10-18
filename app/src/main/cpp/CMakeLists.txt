cmake_minimum_required(VERSION 3.22)
project(codecprobe LANGUAGES C CXX)
# Keep an installable debug APK, but optimize ALL native code including x265.
set(CMAKE_BUILD_TYPE RelWithDebInfo CACHE STRING "" FORCE)
set(CMAKE_C_FLAGS_RELWITHDEBINFO "-O3 -g -DNDEBUG" CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS_RELWITHDEBINFO "-O3 -g -DNDEBUG" CACHE STRING "" FORCE)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
if(ANDROID AND NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "This diagnostic requires arm64-v8a")
endif()
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(ENABLE_SHARED OFF CACHE BOOL "" FORCE)
set(ENABLE_CLI OFF CACHE BOOL "" FORCE)
if(ANDROID OR CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
    set(ENABLE_ASSEMBLY ON CACHE BOOL "" FORCE)
    set(PROBE_ARM_ASM 1)
    if(ANDROID)
        # Upstream custom assembly commands otherwise omit the NDK target.
        set(PROBE_ASM_TARGET_FLAGS "--target=${CMAKE_CXX_COMPILER_TARGET}" "--sysroot=${CMAKE_SYSROOT}")
    endif()
else()
    set(ENABLE_ASSEMBLY OFF CACHE BOOL "" FORCE)
    set(PROBE_ARM_ASM 0)
endif()
set(ENABLE_LIBNUMA OFF CACHE BOOL "" FORCE)
set(ENABLE_PIC ON CACHE BOOL "" FORCE)
set(HIGH_BIT_DEPTH OFF CACHE BOOL "" FORCE)
add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/../../../../vendor/x265/source" x265-build)
if(ANDROID)
    add_library(x265probe SHARED encoder.cpp bridge.cpp stream_encoder.cpp stream_bridge.cpp)
    target_link_options(x265probe PRIVATE "-Wl,-z,max-page-size=16384")
    target_link_libraries(x265probe PRIVATE x265-static log)
else()
    add_executable(x265probe_smoke encoder.cpp smoke.cpp stream_encoder.cpp)
    target_link_libraries(x265probe_smoke PRIVATE x265-static pthread dl)
endif()
set(PROBE_TARGET x265probe)
if(NOT ANDROID)
    set(PROBE_TARGET x265probe_smoke)
endif()
target_include_directories(${PROBE_TARGET} PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../vendor/x265/source"
    "${CMAKE_CURRENT_BINARY_DIR}/x265-build")

target_compile_definitions(${PROBE_TARGET} PRIVATE PROBE_BUILD_TYPE="RelWithDebInfo" PROBE_OPT_LEVEL="O3")

target_compile_definitions(${PROBE_TARGET} PRIVATE PROBE_ARM_ASM=${PROBE_ARM_ASM})


# HDR diagnostic is an independent 10-bit build with a separate cache and DSO.
# The existing 8-bit target, options and converter remain unchanged.
if(ANDROID)
    include(ExternalProject)
    set(HDR_BINARY "${CMAKE_CURRENT_BINARY_DIR}/hdr10-project")
    ExternalProject_Add(x265hdr10_build
        SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/hdr"
        BINARY_DIR "${HDR_BINARY}"
        CMAKE_ARGS -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
            -DANDROID_ABI=${ANDROID_ABI} -DANDROID_PLATFORM=${ANDROID_PLATFORM}
            -DCMAKE_BUILD_TYPE=RelWithDebInfo -DCMAKE_MAKE_PROGRAM=${CMAKE_MAKE_PROGRAM}
        BUILD_COMMAND ${CMAKE_COMMAND} --build <BINARY_DIR> --target x265-static --parallel 2
        INSTALL_COMMAND ""
        BUILD_BYPRODUCTS "${HDR_BINARY}/x26510-build/libx265.a")
    add_library(x265hdr10_static STATIC IMPORTED)
    set_target_properties(x265hdr10_static PROPERTIES IMPORTED_LOCATION "${HDR_BINARY}/x26510-build/libx265.a")
    add_library(x265hdrprobe SHARED hdr/hdr_encoder.cpp hdr/hdr_jni.cpp)
    add_dependencies(x265hdrprobe x265hdr10_build)
    target_include_directories(x265hdrprobe PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/../../../../vendor/x265/source" "${HDR_BINARY}/x26510-build")
    target_link_libraries(x265hdrprobe PRIVATE x265hdr10_static log)
    target_link_options(x265hdrprobe PRIVATE "-Wl,-z,max-page-size=16384" "-Wl,--exclude-libs,ALL" "-Wl,-Bsymbolic")
    set_target_properties(x265hdrprobe PROPERTIES CXX_VISIBILITY_PRESET hidden VISIBILITY_INLINES_HIDDEN YES)
endif()


if(ANDROID)
  add_subdirectory(audio)
endif()

if(ANDROID)
  add_subdirectory(video)
endif()
