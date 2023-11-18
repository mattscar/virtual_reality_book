#ifndef SPINNING_TEXTURES_RENDERER_H_
#define SPINNING_TEXTURES_RENDERER_H_

#include <jni.h>
#include <string>

#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <EGL/egl.h>
#include <GLES3/gl3.h>

#include "vr/gvr/capi/include/gvr.h"

#include "matrixutils.h"
#include "shaderutils.h"

#define NUM_VAOS 1
#define NUM_VBOS 1
#define NUM_IBOS 1
#define NUM_UBOS 1
#define NUM_TEXTURES 1

class SpinningTextureRenderer {

  public:
    SpinningTextureRenderer(AAssetManager* assetMgr, gvr_context_* gvrContext);
    ~SpinningTextureRenderer();

    void OnSurfaceCreated();
    void OnDrawFrame();
    void OnPause();
    void OnResume();
    void InitShader();
    void InitBuffers();
    void InitTexture();
    void RenderEye(gvr::Eye eye, const gvr::BufferViewport& viewport);

  private:

    // Buffer descriptors
    GLuint vaos[NUM_VAOS], vbos[NUM_VBOS], ibos[NUM_IBOS], ubos[NUM_UBOS], tids[NUM_TEXTURES];

    float angle;
    AAssetManager* assetManager;
    std::unique_ptr<gvr::GvrApi> gvrApi;
    std::unique_ptr<gvr::SwapChain> swapChain;
    std::unique_ptr<gvr::BufferViewportList> viewports;
    gvr::BufferViewport buffViewport;
    gvr::Sizei renderSize;
    gvr::Mat4f headMatrix, rotMatrix;

    GLuint program;
    bool ready;
};

#endif  // SPINNING_TEXTURES_RENDERER_H_