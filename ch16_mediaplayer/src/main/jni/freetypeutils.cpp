#include "freetypeutils.h"

static const char* TAG = "MediaPlayer";

TextureAtlas FreeTypeUtils::CreateAtlas(AAssetManager* mgr,
  const char* fileName, unsigned int size) {

  FT_Library ft;
  FT_Face face;
  TextureAtlas atlas;

  // Initialize the library
  FT_Init_FreeType(&ft);

  // Load the font file
  AAsset* asset = 
    AAssetManager_open(mgr, fileName, AASSET_MODE_BUFFER);

  // Access the first face
  FT_New_Memory_Face(ft, (const FT_Byte*)AAsset_getBuffer(asset),
    AAsset_getLength(asset), 0, &face);

  // Set the desired size
  FT_Set_Pixel_Sizes(face, 0, size);

  // Initialize texture atlas properties
  atlas.lineHeight = size;
  float tmp = face->ascender * size * 1.0f/face->height;
  atlas.baselineOffset = (unsigned int)std::round(tmp);
  atlas.textureWidth = 0;

  float top_to_baseline =
    floor(((float)face->ascender/(face->ascender - face->descender)) * size);
  float baseline_to_bottom = ceil(size - top_to_baseline);

  // Iterate through characters
  for(unsigned int i=32; i<128; i++) {

    TextureChar textChar;

    // Load the glyph data
    if(FT_Load_Char(face, i, FT_LOAD_RENDER) != 0) {
      continue;
    }

    if((face->glyph->bitmap.width > 0) && (face->glyph->bitmap.rows > 0)) {

      // Obtain the glyph metrics
      FT_Glyph_Metrics metrics = face->glyph->metrics;

      // Set the properties of the TextureChar
      textChar.x = atlas.textureWidth;
      textChar.width = face->glyph->bitmap.width;
      textChar.height = face->glyph->bitmap.rows;
      textChar.yoffsetHigh = top_to_baseline - face->glyph->bitmap_top;
      textChar.yoffsetLow = baseline_to_bottom - (face->glyph->bitmap.rows - face->glyph->bitmap_top);
      textChar.xoffset = metrics.horiBearingX/64.0f;
      textChar.xadvance = metrics.horiAdvance/64.0f;
      atlas.charMap.emplace(i, textChar);

      // Update the texture width
      atlas.textureWidth += metrics.width/64;
    }
  }

  // Allocate and configure an uninitialized texture
  glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
  glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, atlas.textureWidth,
    size, 0, GL_RED, GL_UNSIGNED_BYTE, 0);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

  // Insert characters into texture
  for(auto const& textureChar: atlas.charMap) {

    // Load the glyph data
    if(FT_Load_Char(face, textureChar.first, FT_LOAD_RENDER) == 0) {

      // Insert the character's bitmap into the texture
      glTexSubImage2D(GL_TEXTURE_2D, 0, textureChar.second.x, 0,
        textureChar.second.width, textureChar.second.height, 
        GL_RED, GL_UNSIGNED_BYTE, face->glyph->bitmap.buffer);
    }
  }

  FT_Done_Face(face);
  FT_Done_FreeType(ft);
  AAsset_close(asset);
  return atlas;
}