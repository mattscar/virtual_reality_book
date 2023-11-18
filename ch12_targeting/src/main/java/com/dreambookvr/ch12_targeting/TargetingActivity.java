package com.dreambookvr.ch12_targeting;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.controller.Controller;
import com.google.vr.sdk.controller.ControllerManager;

public class TargetingActivity extends GvrActivity {

  private Controller controller;
  private ControllerManager manager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set the window properties
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

    // Enable VR mode
    AndroidCompat.setVrModeEnabled(this, true);    
    
    // Access and configure the Google VR view
    setContentView(R.layout.main_layout);
    GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
    gvrView.setEGLContextClientVersion(3);
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    gvrView.setStereoModeEnabled(true);

    // Enable VR mode and sustained performance mode
    if (gvrView.setAsyncReprojectionEnabled(true)) {
      AndroidCompat.setSustainedPerformanceMode(this, true);
    }
    
    // Set renderer and access handler
    TargetingRenderer renderer = new TargetingRenderer(this);
    gvrView.setRenderer(renderer);
    Handler handler = renderer.getHandler();
    setGvrView(gvrView);

    // Access the controller manager
    ControllerListener listener =
      new ControllerListener(handler);    
    manager = new ControllerManager(this, listener);

    // Configure the controller
    controller = manager.getController();
    controller.setEventListener(listener);
    listener.setController(controller);
  }

  @Override
  protected void onStart() {
    super.onStart();
    manager.start();
    controller.update();
  }

  @Override
  protected void onStop() {
    manager.stop();
    super.onStop();
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
}