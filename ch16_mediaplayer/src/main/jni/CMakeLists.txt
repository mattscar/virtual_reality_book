link_directories(${PROJECT_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a)

# Identify the target library and the source files
add_library(mediaplayer SHARED mediaplayer.cpp mediaplayer_renderer.cpp shaderutils.cpp matrixutils.cpp freetypeutils.cpp)

target_compile_options(mediaplayer PUBLIC -std=c++11 -DGL_GLEXT_PROTOTYPES)

include_directories(.)

target_link_libraries(mediaplayer log android EGL GLESv3 gvr gvr_audio freetype)