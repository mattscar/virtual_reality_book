#ifndef MEDIA_PLAYER_RENDERER_H_
#define MEDIA_PLAYER_RENDERER_H_

#include <jni.h>
#include <string>

#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>

#include "vr/gvr/capi/include/gvr.h"
#include "vr/gvr/capi/include/gvr_audio.h"
#include "vr/gvr/capi/include/gvr_controller.h"

#include "matrixutils.h"
#include "shaderutils.h"
#include "freetypeutils.h"

#define NUM_VAOS 4
#define NUM_VBOS 4
#define NUM_IBOS 2
#define NUM_UBOS 1
#define NUM_TEXTURES 2
#define NUM_PROGRAMS 4

class MediaPlayerRenderer {

  public:
    MediaPlayerRenderer(gvr_context_* gvrContext, AAssetManager* assetMgr, 
      std::unique_ptr<gvr::AudioApi>);
    ~MediaPlayerRenderer();

    void OnSurfaceCreated();
    void OnDrawFrame();
    void OnPause();
    void OnResume();
    void InitShaders();
    void InitPlayer();    
    void InitButtons();
    void InitPointer();
    void InitTextures();
    void InitText();
    void RenderEye(gvr::Eye eye, const gvr::BufferViewport& viewport);

  private:
    // Buffer descriptors
    GLuint vaos[NUM_VAOS], vbos[NUM_VBOS], ibos[NUM_IBOS],
      ubos[NUM_UBOS], tids[NUM_TEXTURES], programs[NUM_PROGRAMS];


    AAssetManager* assetManager;
    std::unique_ptr<gvr::GvrApi> gvrApi;
    std::unique_ptr<gvr::SwapChain> swapChain;
    std::unique_ptr<gvr::BufferViewportList> viewports;
    gvr::BufferViewport buffViewport;
    gvr::Sizei renderSize;
    gvr::Mat4f headMatrix;
    int selectedButton;
    bool ready, firstFrame;

    // Extension function
    PFNGLBUFFERSTORAGEEXTPROC glBufferStorageEXT;

    // Controller
    std::unique_ptr<gvr::ControllerApi> controllerApi;
    gvr::ControllerState controllerState;
    float target[2];

    // Audio
    std::unique_ptr<gvr::AudioApi> audioApi;
    gvr::AudioSourceId id;

    // Text
    TextureAtlas atlas;
    GLuint firstIndex;
    GLuint numIndices;
};

#endif  // MEDIA_PLAYER_RENDERER_H_