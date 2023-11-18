package com.dreambookvr.ch03_stringargument;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.TextView;

public class StringArgumentActivity extends Activity {

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create View
    TextView tv = new TextView(this);

    // Access resources
    Resources res = getResources();
    String str = String.format(res.getString(R.string.msg), 4, "John", 3.5);
    tv.setText(str);

    // Configure View properties
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30.0f);
    tv.setBackgroundColor(0xFFFFFF00);
    tv.setTextColor(0xFF000000);    

    // Configure Activity appearance
    setContentView(tv);
  }
}