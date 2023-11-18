package com.dreambookvr.ch14_blankscreen;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import com.google.vr.ndk.base.AndroidCompat;
import com.google.vr.ndk.base.GvrLayout;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BlankScreenActivity extends Activity {
  private GvrLayout gvrLayout;
  private long nativeInst;

  // Load the blankscreen library
  static {
    System.loadLibrary("blankscreen");
  }

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set window properties
    setImmersiveSticky();
    getWindow()
      .getDecorView()
      .setOnSystemUiVisibilityChangeListener(
        new View.OnSystemUiVisibilityChangeListener() {

          @Override
          public void onSystemUiVisibilityChange(int visibility) {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
              setImmersiveSticky();
            }
          }
        });

    // Set the layout
    gvrLayout = new GvrLayout(this);

    // Set sustained performance mode
    AndroidCompat.setVrModeEnabled(this, true);
    if (gvrLayout.setAsyncReprojectionEnabled(true)) {
      AndroidCompat.setSustainedPerformanceMode(this, true);
    }

    // Configure the layout's view
    GLSurfaceView glSurfaceView = new GLSurfaceView(this);
    glSurfaceView.setEGLContextClientVersion(3);
    glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
    glSurfaceView.setPreserveEGLContextOnPause(true);
    gvrLayout.setPresentationView(glSurfaceView);
    setContentView(gvrLayout);    
    
    // Set the renderer
    glSurfaceView.setRenderer(new GLSurfaceView.Renderer() {
      
      public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        nativeOnSurfaceCreated(nativeInst);
      }

      public void onSurfaceChanged(GL10 gl, int w, int h) {}

      public void onDrawFrame(GL10 gl) {
        nativeOnDrawFrame(nativeInst);
      }
    });
    
    // Access the native renderer
    nativeInst = createRenderer(gvrLayout.getGvrApi().
      getNativeGvrContext());    
  }

  // Respond when the activity is paused
  protected void onPause() {
    super.onPause();
    nativeOnPause(nativeInst);
    gvrLayout.onPause();
  }

  // Respond when the activity is resumed
  protected void onResume() {
    super.onResume();
    nativeOnResume(nativeInst);
    gvrLayout.onResume();
  }

  // Respond when the activity is destroyed
  protected void onDestroy() {
    super.onDestroy();
    gvrLayout.shutdown();
    nativeOnDestroy(nativeInst);
  }

  private void setImmersiveSticky() {
    getWindow()
      .getDecorView()
      .setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          | View.SYSTEM_UI_FLAG_FULLSCREEN
          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  }

  // Native methods
  private native long createRenderer(long gvrContext);
  private native void nativeOnSurfaceCreated(long nativeInst);
  private native void nativeOnDrawFrame(long nativeInst);
  private native void nativeOnPause(long nativeInst);
  private native void nativeOnResume(long nativeInst);
  private native void nativeOnDestroy(long nativeInst);
}