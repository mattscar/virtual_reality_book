#ifndef BLANK_SCREEN_RENDERER_H_
#define BLANK_SCREEN_RENDERER_H_

#include <jni.h>

#include <EGL/egl.h>

#include "vr/gvr/capi/include/gvr.h"

class BlankScreenRenderer {
  
  public:
    BlankScreenRenderer(gvr_context_* gvrContext);
    ~BlankScreenRenderer();

    void OnSurfaceCreated();

    void OnDrawFrame();

    void OnPause();

    void OnResume();
    
  private:

    std::unique_ptr<gvr::GvrApi> gvrApi;
};

#endif  // BLANK_SCREEN_RENDERER_H_