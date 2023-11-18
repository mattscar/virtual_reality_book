package com.dreambookvr.ch10_simpletexture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES32;
import android.opengl.GLUtils;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView.StereoRenderer;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

class SimpleTextureRenderer implements StereoRenderer {

  private static final String VERTEX_SHADER = "simpletexture.vert";
  private static final String FRAGMENT_SHADER = "simpletexture.frag";

  private Context context;
  private FloatBuffer vertexBuffer;
  private ByteBuffer indexBuffer;
  private int coordIndex, texcoordIndex;
  private int textureIDs[];
  private Bitmap bitmap;

  // Vertex coordinates and texture coordinates
  static private final float[] vertexData = {
    -0.5f, -0.5f, 0.0f, 1.0f,
     0.5f, -0.5f, 1.0f, 1.0f,
     0.5f,  0.5f, 1.0f, 0.0f,
    -0.5f,  0.5f, 0.0f, 0.0f};

  private static final byte[] indexData = {1, 0, 2, 3};

  SimpleTextureRenderer(Context ctx) {
    context = ctx;

    // Store attribute data in a FloatBuffer
    ByteBuffer tempBuffer =
        ByteBuffer.allocateDirect(vertexData.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = tempBuffer.asFloatBuffer();
    vertexBuffer.put(vertexData);
    vertexBuffer.rewind();

    // Store index data in a ByteBuffer
    indexBuffer = ByteBuffer.allocateDirect(indexData.length);
    indexBuffer.order(ByteOrder.nativeOrder());
    indexBuffer.put(indexData);
    indexBuffer.rewind();

    // Obtain Bitmap from image
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    bitmap = BitmapFactory.decodeResource(
        ctx.getResources(), R.drawable.smiley, options);
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
  }

  private void initTexture() {

    // Generate a texture descriptor
    textureIDs = new int[1];
    GLES32.glGenTextures(1, textureIDs, 0);

    // Make the first texture unit active
    GLES32.glActiveTexture(GLES32.GL_TEXTURE0);

    // Bind the first texture descriptor to its target
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureIDs[0]);

    // Set texture parameters
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_REPEAT);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_REPEAT);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);

    // Read pixel data and associate it with texture
    GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, bitmap, 0);
  }

  @Override
  public void onSurfaceCreated(EGLConfig config) {
    initShader();
    initTexture();

    // Set the region's background color and line width
    GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
  }

  @Override
  public void onDrawEye(Eye eye) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

    // Bind the first texture descriptor to its target
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureIDs[0]);

    // Associate coordinate data with in_coords
    vertexBuffer.rewind();
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
        GLES32.GL_FLOAT, false, 16, vertexBuffer);

    // Associate color data with in_colors
    vertexBuffer.position(2);
    GLES32.glEnableVertexAttribArray(texcoordIndex);
    GLES32.glVertexAttribPointer(texcoordIndex, 2,
        GLES32.GL_FLOAT, false, 16, vertexBuffer);

    // Perform the draw operation
    GLES32.glDrawElements(GLES32.GL_TRIANGLE_STRIP, 4,
        GLES32.GL_UNSIGNED_BYTE, indexBuffer);
  }

  @Override
  public void onSurfaceChanged(int width, int height) {}

  @Override
  public void onNewFrame(HeadTransform headTransform) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {
    GLES32.glDeleteTextures(1, textureIDs, 0);
  }
}
