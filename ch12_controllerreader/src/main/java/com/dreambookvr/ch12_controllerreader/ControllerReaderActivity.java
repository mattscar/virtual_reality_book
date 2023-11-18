package com.dreambookvr.ch12_controllerreader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.controller.Controller;
import com.google.vr.sdk.controller.ControllerManager;

public class ControllerReaderActivity extends Activity {
  private Controller controller;
  private ControllerManager manager;

  // Display results from controller
  static class ControllerHandler extends Handler {
    TextView textView;
    CharSequence[] labels;

    ControllerHandler(TextView tv, CharSequence[] strArray) {
      textView = tv;
      labels = strArray;
    }

    public void handleMessage(Message msg) {
      StringBuilder sb = new StringBuilder();
      Bundle b = msg.getData();

      // App button state
      sb.append(labels[0]).append(": ").
        append(b.getString("APP_MSG")).append("\n\n");

      // Home button state
      sb.append(labels[1]).append(": ").
        append(b.getString("HOME_MSG")).append("\n\n");

      // Click button state
      sb.append(labels[2]).append(": ").
        append(b.getString("CLICK_MSG")).append("\n\n");

      // Orientation as a quaternion
      sb.append(labels[3]).append(": ").
        append(b.getString("QUATERNION_MSG")).append("\n\n");

      // Orientation as a set of angles
      sb.append(labels[4]).append(": ").
        append(b.getString("ANGLE_MSG")).append("\n\n");

      // Connection state
      sb.append(labels[5]).append(": ").
        append(b.getString("CONNECTION_MSG")).append("\n\n");

      // Update textview
      textView.setText(sb.toString());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_layout);

    // Turn on VR mode
    AndroidCompat.setVrModeEnabled(this, true);

    // Access the controller manager
    manager = new ControllerManager(this,
      new ControllerManagerListener());

    // Create the handler
    ControllerHandler handler = new ControllerHandler(
      (TextView)findViewById(R.id.tv), new CharSequence[] {
       getText(R.string.app_msg), getText(R.string.home_msg),
       getText(R.string.click_msg), getText(R.string.quaternion_msg),
       getText(R.string.angle_msg), getText(R.string.connection_msg)});

    // Configure the controller
    ControllerListener listener =
      new ControllerListener(handler);
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

  // Handle ControllerManager events
  private class ControllerManagerListener
    implements ControllerManager.EventListener {

    @Override
    public void onApiStatusChanged(int i) {}

    @Override
    public void onRecentered() {}
  }
}