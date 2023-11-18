package com.dreambookvr.ch03_lifecycle;

import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;

public class LifeCycleActivity extends Activity {
  private TextView tv;

  // Called when the activity is created
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    tv = new TextView(this);
    tv.setText("Created");
    setContentView(tv);
  }

  // Called when the activity is started
  public void onStart() {
    super.onStart();
    tv.append("\nStarted");
  }

  // Called when the activity is restarted
  public void onRestart() {
    super.onRestart();
    tv.append("\nRestarted");
  }

  // Called when the activity is resumed
  public void onResume() {
    super.onResume();
    tv.append("\nResumed");
  }

  // Called when the activity is paused
  public void onPause() {
    super.onPause();
    tv.append("\nPaused");
  }

  // Called when the activity is stopped
  public void onStop() {
    super.onStop();
    tv.append("\nStopped");
  }
}