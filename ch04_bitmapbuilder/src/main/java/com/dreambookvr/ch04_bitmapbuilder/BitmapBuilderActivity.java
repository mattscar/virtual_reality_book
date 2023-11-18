package com.dreambookvr.ch04_bitmapbuilder;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class BitmapBuilderActivity extends Activity {

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Configure bitmap creation options
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inMutable = true;

    // Create bitmap
    Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.smiley, opts);
    
    // Draw border around bitmap
    int w = bm.getWidth();
    int h = bm.getHeight();
    int borderColor = 0xffff0000;
    for(int i=0; i<w; i++) {
      bm.setPixel(i, 0, borderColor);
      bm.setPixel(i, h-1, borderColor);
    }
    for(int i=0; i<h; i++) {
      bm.setPixel(0, i, borderColor);
      bm.setPixel(w-1, i, borderColor);
    }

    // Create the ImageView
    ImageView iv = new ImageView(this);
    iv.setImageBitmap(bm);
    setContentView(iv);
  }
}
