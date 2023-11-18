package com.dreambookvr.ch12_controllerreader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.vr.sdk.controller.Controller;
import com.google.vr.sdk.controller.Controller.ConnectionStates;

class ControllerListener extends Controller.EventListener
  implements Runnable {

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
    b.putString("APP_MSG", controller.appButtonState ? "TRUE" : "FALSE");
    b.putString("HOME_MSG", controller.homeButtonState ? "TRUE" : "FALSE");
    b.putString("CLICK_MSG", controller.clickButtonState ? "TRUE" : "FALSE");
    b.putString("QUATERNION_MSG", controller.orientation.toString());
    b.putString("ANGLE_MSG", controller.orientation.toAxisAngleString());

    switch(connectionCode) {

      // Respond when the controller is disconnected
      case ConnectionStates.DISCONNECTED:
        b.putString("CONNECTION_MSG", "DISCONNECTED");
        break;

      // Respond when the device is searching for the controller
      case ConnectionStates.SCANNING:
        b.putString("CONNECTION_MSG", "SCANNING");
        break;

      // Respond when the device is in the process of connecting
      case ConnectionStates.CONNECTING:
        b.putString("CONNECTION_MSG", "CONNECTING");
        break;

      // Respond when the device has created the connection
      case ConnectionStates.CONNECTED:
        b.putString("CONNECTION_MSG", "CONNECTED");
        break;
    }

    // Send message to UI thread
    handler.sendMessage(msg);

    // Access the controller again
    controller.update();
  }
}
