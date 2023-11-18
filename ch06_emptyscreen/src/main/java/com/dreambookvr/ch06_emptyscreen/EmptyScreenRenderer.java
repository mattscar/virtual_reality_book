package com.dreambookvr.ch06_emptyscreen;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView.StereoRenderer;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

class EmptyScreenRenderer implements StereoRenderer {

  @Override
  public void onSurfaceCreated(EGLConfig config) {}

  @Override
  public void onSurfaceChanged(int width, int height) {}

  @Override
  public void onDrawEye(Eye eye) {}

  @Override
  public void onNewFrame(HeadTransform headTransform) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {}
}
