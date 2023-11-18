package com.dreambookvr.ch10_textdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES32;
import android.opengl.GLUtils;

import com.dreambookvr.ch10_textdemo.AtlasUtils.TextureAtlas;
import com.dreambookvr.ch10_textdemo.AtlasUtils.TextureChar;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView.StereoRenderer;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

class TextDemoRenderer implements StereoRenderer {

  private static final String ATLAS_MSG = "Printed from a texture atlas";
  private static final String CANVAS_MSG = "Printed from a canvas";  
  private static final String VERTEX_SHADER = "textdemo.vert";
  private static final String FRAGMENT_SHADER = "textdemo.frag";

  private Context context;
  private int coordIndex, texcoordIndex, vertScaleLocation;
  private int textureIDs[];
  private float vertScale = 1.0f;

  // Store data for the texture atlas
  private static float[] atlasVertexData;
  private FloatBuffer atlasVertexBuffer;
  private ByteBuffer atlasIndexBuffer;
  private Bitmap atlasBitmap;

  // Store data for the canvas
  private static float[] canvasVertexData = {
    -0.9f,  0.0f, 0.0f, 1.0f,
     0.4f,  0.0f, 1.0f, 1.0f,
     0.4f,  0.15f, 1.0f, 0.0f,
    -0.9f,  0.15f, 0.0f, 0.0f
  };  
  private static byte[] canvasIndexData = {1, 0, 2, 3};
  private FloatBuffer canvasVertexBuffer;
  private ByteBuffer canvasIndexBuffer;
  private Bitmap canvasBitmap;

  TextDemoRenderer(Context ctx) {
    context = ctx;

    // Read texture atlas
    TextureAtlas atlas =
        AtlasUtils.readAtlas(ctx, "droid_sans.fnt");

    // Initialize vertex array for atlas text
    atlasVertexData = new float[16 * ATLAS_MSG.length()];
    atlasVertexData[0] = -0.9f;
    atlasVertexData[1] =  0.35f;
    atlasVertexData[4] = -0.9f;
    atlasVertexData[5] =  0.5f;

    // Initialize index array for atlas text
    byte[] atlasIndexData = new byte[5 * ATLAS_MSG.length()];
    for(byte i=0; i<ATLAS_MSG.length(); i++) {
      atlasIndexData[5*i] = (byte)(4*i+1);
      atlasIndexData[5*i+1] = (byte)(4*i);
      atlasIndexData[5*i+2] = (byte)(4*i+3);
      atlasIndexData[5*i+3] = (byte)(4*i+2);
      atlasIndexData[5*i+4] = (byte)0xff;
    }

    // Add vertices/texture coordinates to list
    generateDataFromAtlas(atlas);

    // Store atlas attribute data
    ByteBuffer tempBuffer =
        ByteBuffer.allocateDirect(atlasVertexData.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    atlasVertexBuffer = tempBuffer.asFloatBuffer();
    atlasVertexBuffer.put(atlasVertexData);
    atlasVertexBuffer.rewind();

    // Store atlas index data
    atlasIndexBuffer = ByteBuffer.allocateDirect(atlasIndexData.length);
    atlasIndexBuffer.order(ByteOrder.nativeOrder());
    atlasIndexBuffer.put(atlasIndexData);
    atlasIndexBuffer.rewind();

    // Obtain Bitmap from texture atlas
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    atlasBitmap = BitmapFactory.decodeResource(
        ctx.getResources(), R.drawable.droid_sans, options);

    // Create Paint object and determine text width
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.setAntiAlias(true);
    paint.setTextSize(32);
    int textWidth = java.lang.Math.round(paint.measureText(CANVAS_MSG));

    // Create Bitmap for Canvas text
    canvasBitmap = Bitmap.createBitmap(textWidth, 33, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(canvasBitmap);
    canvas.drawARGB(255, 255, 255, 255);
    canvas.drawText(CANVAS_MSG, 0.0f, 32.0f, paint);
    
    // Store canvas attribute data
    tempBuffer = ByteBuffer.allocateDirect(canvasVertexData.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    canvasVertexBuffer = tempBuffer.asFloatBuffer();
    canvasVertexBuffer.put(canvasVertexData);
    canvasVertexBuffer.rewind();

    // Store canvas index data
    canvasIndexBuffer = ByteBuffer.allocateDirect(canvasIndexData.length);
    canvasIndexBuffer.order(ByteOrder.nativeOrder());
    canvasIndexBuffer.put(canvasIndexData);
    canvasIndexBuffer.rewind();
  }

  private void generateDataFromAtlas(TextureAtlas atlas) {

    // Determine scaling factor
    float intendedHeight = (float)atlas.lineHeight/(float)atlas.scaleH;
    float actualHeight = java.lang.Math.abs(atlasVertexData[5] - atlasVertexData[1]);
    float scale = actualHeight/intendedHeight;

    // Set character positions
    float currentX = atlasVertexData[0];
    float top = atlasVertexData[5];
    TextureChar currentChar;
    
    for(int i=0; i<ATLAS_MSG.length(); i++) {
      currentChar = atlas.charMap.get((int)ATLAS_MSG.charAt(i));

      // Set texture coordinates
      atlasVertexData[16*i+2] = currentChar.x;
      atlasVertexData[16*i+3] = currentChar.y + currentChar.height;
      atlasVertexData[16*i+6] = currentChar.x;
      atlasVertexData[16*i+7] = currentChar.y;
      atlasVertexData[16*i+10] = currentChar.x + currentChar.width;
      atlasVertexData[16*i+11] = currentChar.y + currentChar.height;
      atlasVertexData[16*i+14] = currentChar.x + currentChar.width;
      atlasVertexData[16*i+15] = currentChar.y;
      
      // Set vertex coordinates
      if(i != 0) {
        atlasVertexData[16*i] = currentX + currentChar.xoffset * scale;
        atlasVertexData[16*i+1] = atlasVertexData[1];
        atlasVertexData[16*i+4] = atlasVertexData[16*i];
        atlasVertexData[16*i+5] = top - currentChar.yoffset * scale;
      } else {
        atlasVertexData[5] -= currentChar.yoffset * scale;
      }
      atlasVertexData[16*i+8] = atlasVertexData[16*i] + currentChar.width * scale;
      atlasVertexData[16*i+9] = atlasVertexData[1];
      atlasVertexData[16*i+12] = atlasVertexData[16*i+8];
      atlasVertexData[16*i+13] = top - currentChar.yoffset * scale;

      // Update current horizontal position
      currentX += currentChar.xadvance * scale;
    }
  }

  private void initShader() {

    // Read and compile vertex shader
    int vertDescriptor = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
    String vertFile = ShaderUtils.readFile(context, VERTEX_SHADER);
    GLES32.glShaderSource(vertDescriptor, vertFile);
    ShaderUtils.compileShader(vertDescriptor);

    // Read and compile fragment shader
    int fragDescriptor = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
    String fragFile = ShaderUtils.readFile(context, FRAGMENT_SHADER);
    GLES32.glShaderSource(fragDescriptor, fragFile);
    ShaderUtils.compileShader(fragDescriptor);

    // Create program and bind attributes
    int program = GLES32.glCreateProgram();
    GLES32.glAttachShader(program, vertDescriptor);
    GLES32.glAttachShader(program, fragDescriptor);

    // Link and use program
    ShaderUtils.linkProgram(program);
    GLES32.glUseProgram(program);

    // Get locations of shader variables
    coordIndex = GLES32.glGetAttribLocation(program, "in_coords");
    texcoordIndex = GLES32.glGetAttribLocation(program, "in_texcoords");
    vertScaleLocation = GLES32.glGetUniformLocation(program, "vertscale");
  }

  private void initTextures() {

    // Generate a texture descriptor
    textureIDs = new int[2];
    GLES32.glGenTextures(2, textureIDs, 0);

    // Make the first texture unit active
    GLES32.glActiveTexture(GLES32.GL_TEXTURE0);

    // Bind the first texture descriptor to its target
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureIDs[0]);

    // Set texture parameters
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);

    // Read pixel data and associate it with texture
    GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, atlasBitmap, 0);
    
    // Bind the second texture descriptor to its target
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureIDs[1]);

    // Set texture parameters
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);

    // Read pixel data and associate it with texture
    GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, canvasBitmap, 0);    
  }

  @Override
  public void onSurfaceCreated(EGLConfig config) {

    // Set the region's background color and line width
    GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

    // Enable primitive restart
    GLES32.glEnable(GLES32.GL_PRIMITIVE_RESTART_FIXED_INDEX);

    // Initialize shader processing
    initShader();
    
    // Initialize texture processing
    initTextures();
  }

  @Override
  public void onSurfaceChanged(int width, int height) {

    // Update vertical scale
    vertScale = (float)width/height;
  }

  @Override
  public void onDrawEye(Eye eye) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

    // Set vertical scale
    GLES32.glUniform1f(vertScaleLocation, vertScale);

    // Associate coordinate data with in_coords
    atlasVertexBuffer.rewind();
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
        GLES32.GL_FLOAT, false, 16, atlasVertexBuffer);

    // Associate texture data with tex_coords
    atlasVertexBuffer.position(2);
    GLES32.glEnableVertexAttribArray(texcoordIndex);
    GLES32.glVertexAttribPointer(texcoordIndex, 2,
        GLES32.GL_FLOAT, false, 16, atlasVertexBuffer);

    // Bind the texture atlas
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureIDs[0]);        
        
    // Draw characters from the texture atlas
    atlasIndexBuffer.rewind();
    GLES32.glDrawElements(GLES32.GL_TRIANGLE_STRIP, ATLAS_MSG.length() * 5,
        GLES32.GL_UNSIGNED_BYTE, atlasIndexBuffer);
        
    // Bind the canvas text
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureIDs[1]);

    // Associate coordinate data with in_coords
    canvasVertexBuffer.rewind();
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
        GLES32.GL_FLOAT, false, 16, canvasVertexBuffer);

    // Associate texture data with tex_coords
    canvasVertexBuffer.position(2);
    GLES32.glEnableVertexAttribArray(texcoordIndex);
    GLES32.glVertexAttribPointer(texcoordIndex, 2,
        GLES32.GL_FLOAT, false, 16, canvasVertexBuffer);

    // Draw characters from the texture atlas
    canvasIndexBuffer.rewind();
    GLES32.glDrawElements(GLES32.GL_TRIANGLE_STRIP, 4,
        GLES32.GL_UNSIGNED_BYTE, canvasIndexBuffer);
  }

  @Override
  public void onNewFrame(HeadTransform headTransform) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {
    GLES32.glDeleteTextures(2, textureIDs, 0);
  }
}
