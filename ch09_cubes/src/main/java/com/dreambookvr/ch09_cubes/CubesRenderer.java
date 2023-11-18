package com.dreambookvr.ch09_cubes;

import android.content.Context;
import android.opengl.GLES32;
import android.opengl.Matrix;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

class CubesRenderer
  implements GvrView.StereoRenderer {

  private static final String VERTEX_SHADER = "cubes.vert";
  private static final String FRAGMENT_SHADER = "cubes.frag";

  private Context context;
  private FloatBuffer vertexBuffer, mvpMatrixBuffer, lightParamBuffer;
  private ByteBuffer indexBuffer;
  private float initMatrix[], viewMatrix[];
  
  // Vertex data
  private static final float[] vertexData = {
    // Left face
    -1.0f, -1.0f, -1.0f,  0.0f,  0.0f,  0.8f, -1.0f,  0.0f,  0.0f,
    -1.0f,  1.0f, -1.0f,  0.0f,  0.0f,  0.8f, -1.0f,  0.0f,  0.0f,
    -1.0f,  1.0f,  1.0f,  0.0f,  0.0f,  0.8f, -1.0f,  0.0f,  0.0f,
    -1.0f, -1.0f,  1.0f,  0.0f,  0.0f,  0.8f, -1.0f,  0.0f,  0.0f,
    // Right face
     1.0f, -1.0f, -1.0f,  0.8f,  0.8f,  0.0f,  1.0f,  0.0f,  0.0f,
     1.0f,  1.0f, -1.0f,  0.8f,  0.8f,  0.0f,  1.0f,  0.0f,  0.0f,
     1.0f,  1.0f,  1.0f,  0.8f,  0.8f,  0.0f,  1.0f,  0.0f,  0.0f,
     1.0f, -1.0f,  1.0f,  0.8f,  0.8f,  0.0f,  1.0f,  0.0f,  0.0f,
    // Top face
    -1.0f, -1.0f, -1.0f,  0.0f,  0.8f,  0.0f,  0.0f, -1.0f,  0.0f,
     1.0f, -1.0f, -1.0f,  0.0f,  0.8f,  0.0f,  0.0f, -1.0f,  0.0f,
     1.0f, -1.0f,  1.0f,  0.0f,  0.8f,  0.0f,  0.0f, -1.0f,  0.0f,
    -1.0f, -1.0f,  1.0f,  0.0f,  0.8f,  0.0f,  0.0f, -1.0f,  0.0f,
    // Bottom face
    -1.0f,  1.0f, -1.0f,  0.8f,  0.0f,  0.8f,  0.0f,  1.0f,  0.0f,
     1.0f,  1.0f, -1.0f,  0.8f,  0.0f,  0.8f,  0.0f,  1.0f,  0.0f,
     1.0f,  1.0f,  1.0f,  0.8f,  0.0f,  0.8f,  0.0f,  1.0f,  0.0f,
    -1.0f,  1.0f,  1.0f,  0.8f,  0.0f,  0.8f,  0.0f,  1.0f,  0.0f,
    // Front face
    -1.0f, -1.0f,  1.0f,  0.8f,  0.0f,  0.0f,  0.0f,  0.0f,  1.0f,
     1.0f, -1.0f,  1.0f,  0.8f,  0.0f,  0.0f,  0.0f,  0.0f,  1.0f,
     1.0f,  1.0f,  1.0f,  0.8f,  0.0f,  0.0f,  0.0f,  0.0f,  1.0f,
    -1.0f,  1.0f,  1.0f,  0.8f,  0.0f,  0.0f,  0.0f,  0.0f,  1.0f,
    // Rear face
    -1.0f, -1.0f, -1.0f,  0.0f,  0.8f,  0.8f,  0.0f,  0.0f, -1.0f,
     1.0f, -1.0f, -1.0f,  0.0f,  0.8f,  0.8f,  0.0f,  0.0f, -1.0f,
     1.0f,  1.0f, -1.0f,  0.0f,  0.8f,  0.8f,  0.0f,  0.0f, -1.0f,
    -1.0f,  1.0f, -1.0f,  0.0f,  0.8f,  0.8f,  0.0f,  0.0f, -1.0f};

  private static final byte[] indexData = {
    0, 1, 3, 2, (byte)0xff,
    7, 6, 4, 5, (byte)0xff,
    11, 10, 8, 9, (byte)0xff,
    12, 13, 15, 14, (byte)0xff,
    19, 18, 16, 17, (byte)0xff,
    20, 21, 23, 22, (byte)0xff};

  private static float[] viewerPos, lightPos;
  private static float shininess;

  private int coordIndex, colorIndex, normalIndex;
  private int mvpMatrixLocation, viewerPosLocation, lightPosLocation;
  private int lightParamsLocation, shininessLocation;

  CubesRenderer(Context ctx) {
    context = ctx;

    // Store vertex data in a FloatBuffer
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

    // Initialize MVP matrix data
    float modelMatrix[] = new float[16];
    initMatrix = new float[16];
    viewMatrix = new float[16];
    Matrix.setIdentityM(modelMatrix, 0);
    Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -10.0f);
    Matrix.rotateM(modelMatrix, 0, -45.0f, 2.0f, 1.0f, -1.0f);

    // Store matrix data in a FloatBuffer
    tempBuffer = ByteBuffer.allocateDirect(16 * 4 * 3);
    tempBuffer.order(ByteOrder.nativeOrder());
    mvpMatrixBuffer = tempBuffer.asFloatBuffer();
    mvpMatrixBuffer.put(modelMatrix);

    // Store light parameters in a FloatBuffer
    float lightParams[] = new float[]
        {0.05f, 0.05f, 0.05f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    tempBuffer =
        ByteBuffer.allocateDirect(lightParams.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    lightParamBuffer = tempBuffer.asFloatBuffer();
    lightParamBuffer.put(lightParams);
    lightParamBuffer.rewind();

    // Set data for other shader variables
    viewerPos = new float[] {0.0f, 0.0f, 0.0f};
    lightPos = new float[] {-5.0f, -5.0f, -20.0f};
    shininess = 0.1f;
  }

  @Override
  public void onSurfaceCreated(EGLConfig config) {
    int program;

    // Set the region's background color and line width
    GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    GLES32.glClearDepthf(0.997f);

    // Configure polygon culling
    GLES32.glEnable(GLES32.GL_CULL_FACE);
    GLES32.glFrontFace(GLES32.GL_CW);
    GLES32.glCullFace(GLES32.GL_BACK);

    // Configure depth testing
    GLES32.glEnable(GLES32.GL_DEPTH_TEST);
    GLES32.glDepthFunc(GLES32.GL_LESS);

    // Enable primitive restart
    GLES32.glEnable(GLES32.GL_PRIMITIVE_RESTART_FIXED_INDEX);

    // Read and compile vertex shader
    int vertDescriptor =
      GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
    String vertFile = ShaderUtils.readFile(context, VERTEX_SHADER);
    GLES32.glShaderSource(vertDescriptor, vertFile);
    ShaderUtils.compileShader(vertDescriptor);

    // Read and compile fragment shader
    int fragDescriptor =
      GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
    String fragFile =
      ShaderUtils.readFile(context, FRAGMENT_SHADER);
    GLES32.glShaderSource(fragDescriptor, fragFile);
    ShaderUtils.compileShader(fragDescriptor);

    // Create program and bind attributes
    program = GLES32.glCreateProgram();
    GLES32.glAttachShader(program, vertDescriptor);
    GLES32.glAttachShader(program, fragDescriptor);

    // Link and use program
    ShaderUtils.linkProgram(program);
    GLES32.glUseProgram(program);

    coordIndex =
        GLES32.glGetAttribLocation(program, "in_coords");
    colorIndex =
        GLES32.glGetAttribLocation(program, "in_color");
    normalIndex =
        GLES32.glGetAttribLocation(program, "in_normal");

    mvpMatrixLocation =
        GLES32.glGetUniformLocation(program, "mvp_matrices");
    viewerPosLocation =
        GLES32.glGetUniformLocation(program, "viewer_pos");
    lightPosLocation =
        GLES32.glGetUniformLocation(program, "light_pos");
    lightParamsLocation =
        GLES32.glGetUniformLocation(program, "light_params");
    shininessLocation =
        GLES32.glGetUniformLocation(program, "shininess");
  }

  @Override
  public void onNewFrame(HeadTransform headTransform) {

    // Initialize the view matrix
    Matrix.setLookAtM(initMatrix, 0, 0.0f, 0.0f, 0.01f,
        0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
  }

  @Override
  public void onDrawEye(Eye eye) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT |
        GLES32.GL_DEPTH_BUFFER_BIT);

    // Set view/projection matrix
    mvpMatrixBuffer.position(16);
    Matrix.multiplyMM(viewMatrix, 0,
        eye.getEyeView(), 0, initMatrix, 0);
    mvpMatrixBuffer.put(viewMatrix);
    mvpMatrixBuffer.put(eye.getPerspective(0.1f, 100.0f));

    // Associate coordinate data with in_coords
    vertexBuffer.rewind();
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 3,
      GLES32.GL_FLOAT, false, 36, vertexBuffer);

    // Associate color data with in_colors
    vertexBuffer.position(3);
    GLES32.glEnableVertexAttribArray(colorIndex);
    GLES32.glVertexAttribPointer(colorIndex, 3,
      GLES32.GL_FLOAT, false, 36, vertexBuffer);

    // Associate normal vector components with in_normal
    vertexBuffer.position(6);
    GLES32.glEnableVertexAttribArray(normalIndex);
    GLES32.glVertexAttribPointer(normalIndex, 3,
      GLES32.GL_FLOAT, false, 36, vertexBuffer);

    // Associate MVP matrices with mvp_matrices
    mvpMatrixBuffer.rewind();
    GLES32.glUniformMatrix4fv(mvpMatrixLocation,
      3, false, mvpMatrixBuffer);

    // Associate vector with viewer_pos
    GLES32.glUniform3f(viewerPosLocation, viewerPos[0],
      viewerPos[1], viewerPos[2]);

    // Associate vector with light_pos
    GLES32.glUniform3f(lightPosLocation, lightPos[0],
      lightPos[1], lightPos[2]);

    // Associate vectors with light_params
    GLES32.glUniform3fv(lightParamsLocation, 3, lightParamBuffer);

    // Associate value with shininess
    GLES32.glUniform1f(shininessLocation, shininess);

    // Draw six faces of each cube
    GLES32.glDrawElementsInstanced(GLES32.GL_TRIANGLE_STRIP, 24,
        GLES32.GL_UNSIGNED_BYTE, indexBuffer, 3);
  }

  @Override
  public void onSurfaceChanged(int width, int height) {}  
  
  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {}
}