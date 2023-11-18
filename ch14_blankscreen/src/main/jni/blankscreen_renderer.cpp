#include <android/log.h>

#include "blankscreen_renderer.h"

BlankScreenRenderer::BlankScreenRenderer(gvr_context* gvrContext):
  gvrApi(gvr::GvrApi::WrapNonOwned(gvrContext)) {}
  
BlankScreenRenderer::~BlankScreenRenderer() {}

// Called when the surface is created
void BlankScreenRenderer::OnSurfaceCreated() {

  // Print a message to the log
  __android_log_write(ANDROID_LOG_INFO, "BlankScreenRenderer", "Creating the surface");

  // Access the GvrApi structure
  gvrApi->InitializeGl();
}

// Called to draw a frame
void BlankScreenRenderer::OnDrawFrame() {}

// Called when the activity is paused
void BlankScreenRenderer::OnPause() {}

// Called when the activity is resumed
void BlankScreenRenderer::OnResume() {}