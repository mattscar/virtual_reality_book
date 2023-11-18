package com.dreambookvr.ch13_audioplayer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.vr.sdk.controller.Controller;
import com.google.vr.sdk.controller.Controller.ConnectionStates;
import com.google.vr.sdk.controller.ControllerManager;

class ControllerListener extends Controller.EventListener
  implements Runnable, ControllerManager.EventListener {

  private Handler handler;
  private Controller controller;
  private int connectionCode = ConnectionStates.DISCONNECTED;

  ControllerListener(Handler h) {
    handler = h;
  }

  void setController(Controller c) {
    controller = c;
  }

  @Override
  public void onConnectionStateChanged(int state) {
    connectionCode = state;
    handler.post(this);
  }

  @Override
  public void onUpdate() {
    handler.post(this);
  }

  @Override
  public void run() {

    // Configure a Message for the UI thread
    Message msg = Message.obtain(handler);
    Bundle b = msg.getData();

    // Store quaternion components
    float[] quaternion = {controller.orientation.w, controller.orientation.x,
        controller.orientation.y, controller.orientation.z};
    b.putFloatArray("QUAT", quaternion);

    // Store connection state
    switch(connectionCode) {
      case ConnectionStates.DISCONNECTED:
        b.putString("CONNECTION_MSG", "DISCONNECTED");
        break;
      case ConnectionStates.SCANNING:
        b.putString("CONNECTION_MSG", "SCANNING");
        break;
      case ConnectionStates.CONNECTING:
        b.putString("CONNECTION_MSG", "CONNECTING");
        break;
      case ConnectionStates.CONNECTED:
        b.putString("CONNECTION_MSG", "CONNECTED");
        break;
    }

    // Store App button state
    b.putBoolean("APP_KEY_PRESSED", controller.appButtonState);

    // Send message to UI thread
    handler.sendMessage(msg);

    // Access the controller again
    controller.update();
  }
  
  @Override
  public void onApiStatusChanged(int i) {}

  @Override
  public void onRecentered() {}  
}
