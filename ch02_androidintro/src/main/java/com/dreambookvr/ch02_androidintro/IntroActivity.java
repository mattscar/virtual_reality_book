package com.dreambookvr.ch02_androidintro;

import android.app.Activity;
import android.os.Bundle;

public class IntroActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_layout);
  }
}
