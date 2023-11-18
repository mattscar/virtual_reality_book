package com.dreambookvr.ch05_basichandler;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class BasicHandlerActivity extends Activity
             implements Runnable {

  // Configure Handler to display message content
  static class MsgHandler extends Handler {
    final TextView textView;

    MsgHandler(TextView view) {
      textView = view;
    }

    public void handleMessage(Message msg) {
      String str = "Arg1: " + msg.arg1 +
          "\nArg2: " + msg.arg2;
      textView.setText(str);
    }
  }
  private MsgHandler handler;

  public void onCreate(Bundle b) {

    super.onCreate(b);
    setContentView(R.layout.main_layout);
    TextView tv = (TextView)findViewById(R.id.tv);
    
    // Create and launch the thread
    handler = new MsgHandler(tv);
    Thread t = new Thread(this);
    t.start();
  }
  
  public void run() {
    
    // Create and configure a new Message
    Message msg = Message.obtain(handler);
    msg.arg1 = 21;
    msg.arg2 = 37;

    // Send message to handler
    handler.sendMessage(msg);
  }
}