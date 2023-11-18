#ifndef FREETYPE_UTILS_H_
#define FREETYPE_UTILS_H_

#include <cmath>
#include <cstdlib>
#include <map>
#include <string>
#include <vector>

#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <GLES3/gl3.h>

#include "ft2build.h"
#include FT_FREETYPE_H

typedef struct {
  unsigned int x, width, height;
  float aspect, xoffset, yoffsetLow, yoffsetHigh, xadvance;
} TextureChar;

typedef struct {
  unsigned int lineHeight;
  unsigned int baselineOffset;
  unsigned int textureWidth;
  std::map<unsigned int, TextureChar> charMap;
} TextureAtlas;

class FreeTypeUtils {
  
  public:
  
    // Create a texture atlas from the font file
    static TextureAtlas CreateAtlas(AAssetManager* mgr, const char* fileName, unsigned int size);
};

#endif  // FREETYPE_UTILS_H_