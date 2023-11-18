package com.dreambookvr.ch07_orangescreen;

import android.opengl.GLES32;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView.StereoRenderer;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

class OrangeScreenRenderer implements StereoRenderer {

  @Override
  public void onSurfaceCreated(EGLConfig config) {

    // Set the region's background color and line width
    GLES32.glClearColor(1.0f, 0.5f, 0.0f, 1.0f);
  }

  @Override
  public void onNewFrame(HeadTransform headTransform) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);
  }

  @Override
  public void onDrawEye(Eye eye) {}

  @Override
  public void onSurfaceChanged(int width, int height) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {}
}
