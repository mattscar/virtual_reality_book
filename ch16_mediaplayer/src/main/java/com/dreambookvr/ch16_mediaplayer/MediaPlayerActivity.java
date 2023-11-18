package com.dreambookvr.ch16_mediaplayer;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import com.google.vr.ndk.base.AndroidCompat;
import com.google.vr.ndk.base.GvrLayout;

import java.lang.ClassLoader;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MediaPlayerActivity extends Activity {

  private GvrLayout gvrLayout;
  private long nativeInst;

  // Load the native library  
  static {
    System.loadLibrary("mediaplayer");
  }

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set the window properties
    getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    // Set the layout
    AndroidCompat.setVrModeEnabled(this, true);
    gvrLayout = new GvrLayout(this);
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
    nativeInst = createRenderer(
      gvrLayout.getGvrApi().getNativeGvrContext(), getAssets(),
      getClass().getClassLoader(), this.getApplicationContext());
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

  // Native methods
  private native long createRenderer(long gvrContext, AssetManager manager, 
    ClassLoader loader, Context context);
  private native void nativeOnSurfaceCreated(long nativeInst);
  private native void nativeOnDrawFrame(long nativeInst);
  private native void nativeOnPause(long nativeInst);
  private native void nativeOnResume(long nativeInst);
  private native void nativeOnDestroy(long nativeInst);
}