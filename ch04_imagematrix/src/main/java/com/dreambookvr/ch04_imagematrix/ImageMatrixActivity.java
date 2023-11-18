package com.dreambookvr.ch04_imagematrix;

import android.app.Activity;
import android.graphics.Matrix;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageMatrixActivity extends Activity {

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Create and configure the Matrix
    Matrix m = new Matrix();
    m.preTranslate(200.0f, 100.0f);
    m.preRotate(45.0f);
    
    // Create the ImageView
    ImageView iv = new ImageView(this);
    iv.setImageResource(R.drawable.smiley);
    iv.setScaleType(ScaleType.MATRIX);
    iv.setImageMatrix(m);
    
    setContentView(iv);
  }
}
