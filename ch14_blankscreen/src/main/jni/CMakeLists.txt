# Identify the directory containing the Google VR libraries
link_directories(${PROJECT_SOURCE_DIR}/src/main/jniLibs/armeabi-v7a)

# Identify the target library and the source code
add_library(blankscreen SHARED blankscreen.cpp blankscreen_renderer.cpp)

# Enable C++11 processing
target_compile_options(blankscreen PRIVATE -std=c++11)

# Tell CMake to look for header files in the current directory
include_directories(.)

# Identify libraries to be linked into the build
target_link_libraries(blankscreen log EGL gvr)