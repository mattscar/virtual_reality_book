package com.dreambookvr.ch15_spinningtexture;

import android.app.Activity;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import com.google.vr.ndk.base.AndroidCompat;
import com.google.vr.ndk.base.GvrLayout;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SpinningTextureActivity extends Activity {

  private GvrLayout gvrLayout;
  private long nativeInst;

  // Load the native library  
  static {
    System.loadLibrary("spinningtexture");
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
    glSurfaceView.setEGLConfigChooser(8, 8, 8, 0, 0, 0);
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
    nativeInst = createRenderer(getAssets(),
        gvrLayout.getGvrApi().getNativeGvrContext());
  }

  @Override
  protected void onPause() {
    super.onPause();
    nativeOnPause(nativeInst);
    gvrLayout.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    nativeOnResume(nativeInst);
    gvrLayout.onResume();
  }

  @Override
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
  private native long createRenderer(AssetManager manager, long gvrContext);
  private native void nativeOnSurfaceCreated(long nativeInst);
  private native void nativeOnDrawFrame(long nativeInst);
  private native void nativeOnPause(long nativeInst);
  private native void nativeOnResume(long nativeInst);
  private native void nativeOnDestroy(long nativeInst);
}