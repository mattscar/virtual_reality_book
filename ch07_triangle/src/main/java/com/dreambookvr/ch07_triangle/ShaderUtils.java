package com.dreambookvr.ch07_triangle;

import android.content.Context;
import android.opengl.GLES32;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class ShaderUtils {

  private static final String TAG = "Triangle";

  static String readFile(Context context, String fileName) {

    // Read file
    String line;
    StringBuilder sb = new StringBuilder();

    try {
      InputStream input = context.getAssets().open(fileName);
      BufferedReader buf =
        new BufferedReader(new InputStreamReader(input));
      while((line = buf.readLine()) != null){
        sb.append(line).append("\n");
      }
      input.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  static void compileShader(int shader) {
    int[] result = new int[1];

    GLES32.glCompileShader(shader);
    GLES32.glGetShaderiv(shader, GLES32.GL_COMPILE_STATUS, result, 0);
    if(result[0] == GLES32.GL_FALSE) {
      Log.v(TAG, GLES32.glGetShaderInfoLog(shader));
      System.exit(1);
    }
  }

  static void linkProgram(int program) {
    int[] result = new int[1];

    GLES32.glLinkProgram(program);
    GLES32.glGetProgramiv(program, GLES32.GL_LINK_STATUS, result, 0);
    if(result[0] == GLES32.GL_FALSE) {
      Log.v(TAG, GLES32.glGetProgramInfoLog(program));
      System.exit(1);
    }
  }
}
