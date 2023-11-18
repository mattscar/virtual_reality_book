package com.dreambookvr.ch08_fourtriangles;

import android.os.Bundle;
import android.view.View;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;

public class FourTrianglesActivity extends GvrActivity {

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
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
    gvrView.setStereoModeEnabled(false);
    gvrView.setRenderer(new FourTrianglesRenderer(this));
    setGvrView(gvrView);
    
    // Set sustained performance mode
    if (gvrView.setAsyncReprojectionEnabled(true)) {
      AndroidCompat.setSustainedPerformanceMode(this, true);
    }    
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