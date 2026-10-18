include(ExternalProject)
set(FFMPEG_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../vendor/ffmpeg-8.1.3")
set(FFMPEG_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg-install")
file(MAKE_DIRECTORY "${FFMPEG_PREFIX}/include")
# NDK clang from the parent project retains its Android target via the driver name.
get_filename_component(LLVM_BIN "${CMAKE_C_COMPILER}" DIRECTORY)
set(FFMPEG_CC "${LLVM_BIN}/aarch64-linux-android${CMAKE_SYSTEM_VERSION}-clang")
ExternalProject_Add(softwarehevc_build
    SOURCE_DIR "${FFMPEG_SOURCE}"
    BINARY_DIR "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg-build"
    CONFIGURE_COMMAND ""
    BUILD_COMMAND bash "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../scripts/build-video-decoder.sh"
        "${FFMPEG_SOURCE}" "${CMAKE_CURRENT_BINARY_DIR}/ffmpeg-build" "${FFMPEG_PREFIX}" "${FFMPEG_CC}"
    INSTALL_COMMAND ""
    BUILD_BYPRODUCTS "${FFMPEG_PREFIX}/lib/libavcodec.a" "${FFMPEG_PREFIX}/lib/libavutil.a")
add_library(softwarehevc SHARED hevc_decoder.cpp decoder_jni.cpp)
add_dependencies(softwarehevc softwarehevc_build)
target_include_directories(softwarehevc PRIVATE "${FFMPEG_PREFIX}/include")
target_link_libraries(softwarehevc PRIVATE "${FFMPEG_PREFIX}/lib/libavcodec.a" "${FFMPEG_PREFIX}/lib/libavutil.a" m)
target_link_options(softwarehevc PRIVATE "-Wl,-z,max-page-size=16384" "-Wl,--exclude-libs,ALL" "-Wl,-Bsymbolic")
set_target_properties(softwarehevc PROPERTIES CXX_VISIBILITY_PRESET hidden VISIBILITY_INLINES_HIDDEN YES)
