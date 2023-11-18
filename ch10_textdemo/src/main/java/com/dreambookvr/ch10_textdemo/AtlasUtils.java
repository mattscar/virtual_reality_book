package com.dreambookvr.ch10_textdemo;

import android.content.Context;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class AtlasUtils {

  static class TextureChar {
    float x, y;
    float width, height;
    float xoffset, yoffset, xadvance;
  }

  static class TextureAtlas {
    short lineHeight;
    short baselineOffset;
    short scaleW, scaleH;
    short numChars;
    SparseArray<TextureChar> charMap;
  }

  static TextureAtlas readAtlas(Context context, String fileName) {

    TextureChar textureChar;
    InputStream input;
    String line;
    int index;

    TextureAtlas atlas = new TextureAtlas();

    // Read data from generated font file
    try {
      input = context.getAssets().open(fileName);
      BufferedReader buf =
        new BufferedReader(new InputStreamReader(input));
      while((line = buf.readLine()) != null) {

        // Process common line
        if(line.startsWith("common")) {
          String[] tokens = line.split("\\s+");

          // Get line height
          atlas.lineHeight = Short.valueOf(tokens[1].split("=")[1]);

          // Get baseline offset
          atlas.baselineOffset = Short.valueOf(tokens[2].split("=")[1]);

          // Get texture width
          atlas.scaleW = Short.valueOf(tokens[3].split("=")[1]);

          // Get texture height
          atlas.scaleH = Short.valueOf(tokens[4].split("=")[1]);
        }

        // Determine number of characters
        else if(line.startsWith("chars")) {
          String[] tokens = line.split("\\s+");
          atlas.numChars = Short.valueOf(tokens[1].split("=")[1]);
          atlas.charMap = new SparseArray<>(atlas.numChars);
        }

        // Obtain character
        else if(line.startsWith("char")) {
          String[] tokens = line.split("\\s+");

          // Allocate memory for character
          textureChar = new TextureChar();

          // Get character's location
          textureChar.x = Float.valueOf(tokens[2].split("=")[1])/atlas.scaleW;
          textureChar.y = Float.valueOf(tokens[3].split("=")[1])/atlas.scaleH;

          // Get character's dimensions
          textureChar.width = Float.valueOf(tokens[4].split("=")[1])/atlas.scaleW;
          textureChar.height = Float.valueOf(tokens[5].split("=")[1])/atlas.scaleH;

          // Get character's offsets
          textureChar.xoffset = Float.valueOf(tokens[6].split("=")[1])/atlas.scaleW;
          textureChar.yoffset = Float.valueOf(tokens[7].split("=")[1])/atlas.scaleH;
          textureChar.xadvance = Float.valueOf(tokens[8].split("=")[1])/atlas.scaleW;

          // Put character in map
          index = Integer.valueOf(tokens[1].split("=")[1]);
          atlas.charMap.put(index, textureChar);
        }
      }
      buf.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return atlas;
  }
}
