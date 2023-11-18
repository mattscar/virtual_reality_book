package com.dreambookvr.ch12_targeting;

import android.content.Context;
import android.opengl.GLES32;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;

class TargetingRenderer
  implements GvrView.StereoRenderer {
  
  private static final String SQUARE_VERTEX_SHADER = "square.vert";
  private static final String CIRCLE_VERTEX_SHADER = "circle.vert";
  private static final String FRAGMENT_SHADER = "targeting.frag";

  // Object counts
  private static final int NUM_VAOS = 2;
  private static final int NUM_VBOS = 2;
  private static final int NUM_UBOS = 1;
  private static final int NUM_IBOS = 1;

  private static final float SQUARE_DEPTH = -5.0f;
  private static final float CIRCLE_RADIUS = 0.05f;

  // Vertex coordinates and texture coordinates
  private static final float[] squareVertices = {
    -0.20f, -0.20f, SQUARE_DEPTH,
     0.20f, -0.20f, SQUARE_DEPTH,
     0.20f,  0.20f, SQUARE_DEPTH,
    -0.20f,  0.20f, SQUARE_DEPTH};

  // Vertex indices
  private static final byte[] indexData = {1, 0, 2, 3};

  // Square positions
  private static final float[] offsets = {
    -0.75f, -0.75f, -0.75f, -0.25f,
    -0.75f,  0.25f, -0.75f,  0.75f,
    -0.25f, -0.75f, -0.25f, -0.25f,
    -0.25f,  0.25f, -0.25f,  0.75f,
     0.25f, -0.75f,  0.25f, -0.25f,
     0.25f,  0.25f,  0.25f,  0.75f,
     0.75f, -0.75f,  0.75f, -0.25f,
     0.75f,  0.25f,  0.75f,  0.75f};

  private Context context;
  private int[] programs, vaos, vbos, ibos, ubos;
  private float[] headMatrix, viewMatrix, mvpMatrix;
  private float[] target, circleVertices;
  private FloatBuffer offsetBuffer, mvpBuffer, targetBuffer;
  private IntBuffer selectBuffer;
  static private float[] quat;
  static private boolean connected;

  // Display results from controller
  static private class ControllerHandler extends Handler {
    public void handleMessage(Message msg) {
      Bundle b = msg.getData();
      connected = b.getString("CONNECTION_MSG", "").equals("CONNECTED");
      quat = b.getFloatArray("QUAT");
    }
  }
  private ControllerHandler handler = new ControllerHandler();

  TargetingRenderer(Context ctx) {
    context = ctx;

    // Initialize object arrays
    vaos = new int[NUM_VAOS];
    vbos = new int[NUM_VBOS];
    ibos = new int[NUM_IBOS];
    ubos = new int[NUM_UBOS];

    // Initialize other arrays
    programs = new int[2];
    headMatrix = new float[16];
    viewMatrix = new float[16];
    mvpMatrix = new float[16];
    quat = new float[4];
    target = new float[2];

    // Initialize circle vertices
    circleVertices = new float[48];
    circleVertices[0] = 0.0f;
    circleVertices[1] = 0.0f;
    circleVertices[2] = SQUARE_DEPTH + 0.1f;
    for(int i=0; i<15; i++) {
      circleVertices[3*(i+1)] = CIRCLE_RADIUS * (float)Math.cos((2.0f * Math.PI * i)/14);
      circleVertices[3*(i+1)+1] = CIRCLE_RADIUS * (float)Math.sin((2.0f * Math.PI * i)/14);
      circleVertices[3*(i+1)+2] = SQUARE_DEPTH + 0.1f;
    }

    // Set offset data
    ByteBuffer tempBuffer =
      ByteBuffer.allocateDirect(offsets.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    offsetBuffer = tempBuffer.asFloatBuffer();
    offsetBuffer.put(offsets);
    offsetBuffer.rewind();

    // Create buffer for matrix data
    tempBuffer =
      ByteBuffer.allocateDirect(mvpMatrix.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    mvpBuffer = tempBuffer.asFloatBuffer();
    mvpBuffer.rewind();

    // Create buffer to store target
    target[0] = 0.0f; target[1] = 0.0f;
    tempBuffer =
        ByteBuffer.allocateDirect(target.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    targetBuffer = tempBuffer.asFloatBuffer();
    targetBuffer.put(target);
    targetBuffer.rewind();

    // Create buffer to store selected index
    tempBuffer = ByteBuffer.allocateDirect(4);
    tempBuffer.order(ByteOrder.nativeOrder());
    selectBuffer = tempBuffer.asIntBuffer();
  }

  private void initShaders() {

    // Read and compile square vertex shader
    int vertDescriptor = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
    String vertFile = ShaderUtils.readFile(context, SQUARE_VERTEX_SHADER);
    GLES32.glShaderSource(vertDescriptor, vertFile);
    ShaderUtils.compileShader(vertDescriptor);

    // Read and compile square fragment shader
    int fragDescriptor = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
    String fragFile = ShaderUtils.readFile(context, FRAGMENT_SHADER);
    GLES32.glShaderSource(fragDescriptor, fragFile);
    ShaderUtils.compileShader(fragDescriptor);

    // Create and configure square program
    programs[0] = GLES32.glCreateProgram();
    GLES32.glAttachShader(programs[0], vertDescriptor);
    GLES32.glAttachShader(programs[0], fragDescriptor);
    ShaderUtils.linkProgram(programs[0]);

    // Read and compile circle vertex shader
    vertDescriptor = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
    vertFile = ShaderUtils.readFile(context, CIRCLE_VERTEX_SHADER);
    GLES32.glShaderSource(vertDescriptor, vertFile);
    ShaderUtils.compileShader(vertDescriptor);

    // Create and configure circle program
    programs[1] = GLES32.glCreateProgram();
    GLES32.glAttachShader(programs[1], vertDescriptor);
    GLES32.glAttachShader(programs[1], fragDescriptor);
    ShaderUtils.linkProgram(programs[1]);
  }

  private void initSquareData() {

    // Set the VAO
    GLES32.glBindVertexArray(vaos[0]);

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[0]);
    int dataSize = squareVertices.length * 4;
    GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
        dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    ByteBuffer buff = (ByteBuffer)GLES32.glMapBufferRange(
        GLES32.GL_ARRAY_BUFFER, 0, dataSize,
        GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    buff.asFloatBuffer().put(squareVertices);
    GLES32.glUnmapBuffer(GLES32.GL_ARRAY_BUFFER);

    // Associate coordinate data with in_coords
    int coordIndex = GLES32.glGetAttribLocation(programs[0], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 3,
      GLES32.GL_FLOAT, false, 0, 0);

    // Configure the IBO
    GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, ibos[0]);
    GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER,
      indexData.length, null, GLES32.GL_DYNAMIC_DRAW);
    buff = (ByteBuffer)GLES32.glMapBufferRange(GLES32.GL_ELEMENT_ARRAY_BUFFER,
      0, indexData.length, GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.put(indexData);
    GLES32.glUnmapBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER);

    // Configure the UBO
    GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER, ubos[0]);
    GLES32.glBufferData(GLES32.GL_UNIFORM_BUFFER,
      (mvpMatrix.length + target.length + offsets.length + 1) * 4,
      null, GLES32.GL_DYNAMIC_DRAW);
    GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,
      (mvpMatrix.length + target.length) * 4, offsets.length * 4, offsetBuffer);

    // Associate offset data with the ubo block
    int bindingPoint = 1;
    int uboIndex = GLES32.glGetUniformBlockIndex(programs[0], "ubo");
    GLES32.glUniformBlockBinding(programs[0], uboIndex, bindingPoint);
    GLES32.glBindBufferBase(GLES32.GL_UNIFORM_BUFFER, bindingPoint, ubos[0]);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }
  
  private void initPointerData() {

    // Set the program and VBO
    GLES32.glBindVertexArray(vaos[1]);

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[1]);
    int dataSize = circleVertices.length * 4;
    GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
        dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    ByteBuffer buff = (ByteBuffer)GLES32.glMapBufferRange(
        GLES32.GL_ARRAY_BUFFER, 0, dataSize,
        GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    buff.asFloatBuffer().put(circleVertices);
    GLES32.glUnmapBuffer(GLES32.GL_ARRAY_BUFFER);

    // Associate coordinate data with in_coords
    int coordIndex = GLES32.glGetAttribLocation(programs[1], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 3,
        GLES32.GL_FLOAT, false, 0, 0);

    // Associate offset data with the ubo block
    int bindingPoint = 2;
    int uboIndex = GLES32.glGetUniformBlockIndex(programs[1], "ubo");
    GLES32.glUniformBlockBinding(programs[1], uboIndex, bindingPoint);
    GLES32.glBindBufferBase(GLES32.GL_UNIFORM_BUFFER, bindingPoint, ubos[0]);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  @Override
  public void onSurfaceCreated(EGLConfig config) {

    GLES32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    // Configure shaders
    initShaders();

    // Generate objects
    GLES32.glGenVertexArrays(NUM_VAOS, vaos, 0);
    GLES32.glGenBuffers(NUM_VBOS, vbos, 0);
    GLES32.glGenBuffers(NUM_IBOS, ibos, 0);
    GLES32.glGenBuffers(NUM_UBOS, ubos, 0);

    // Configure drawing data
    initSquareData();
    initPointerData();
  }

  @Override
  public void onNewFrame(HeadTransform headTransform) {

    // Determine where the controller is pointing
    float tmp = (quat[0] * quat[0]) - (quat[1] * quat[1]) - (quat[2] * quat[2]) + (quat[3] * quat[3]);
    target[0] = (2.0f * (quat[0] * quat[2]) - (2 * quat[1] * quat[3])) * (SQUARE_DEPTH / tmp);
    target[1] = (-2.0f * (quat[0] * quat[1]) - (2 * quat[2] * quat[3])) * (SQUARE_DEPTH / tmp);

    // Update the UBO with the target information
    targetBuffer.put(target);
    targetBuffer.rewind();
    GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER, ubos[0]);
    GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,
        mvpMatrix.length * 4, target.length * 4, targetBuffer);

    // Determine which square is selected
    int selectedSquare = -1;
    for(int i=0; i<offsets.length; i+=2) {
      if((target[0] > (offsets[i] - 0.2f)) &&
          (target[0] < (offsets[i] + 0.2f)) &&
          (target[1] > (offsets[i+1] - 0.2f)) &&
          (target[1] < (offsets[i+1] + 0.2f))) {
        selectedSquare = i/2;
        break;
      }
    }

    // Update the UBO with the selected square
    selectBuffer.put(selectedSquare);
    selectBuffer.rewind();
    GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,
        (mvpMatrix.length + target.length + offsets.length) * 4, 4, selectBuffer);

    // Initialize the view matrix
    Matrix.setLookAtM(headMatrix, 0, 0.0f, 0.0f, 0.01f,
        0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
  }

  private void drawSquare(Eye eye) {

    // Set the program and VAO
    GLES32.glBindVertexArray(vaos[0]);
    GLES32.glUseProgram(programs[0]);

    // Set model-view-projection matrix
    Matrix.multiplyMM(viewMatrix, 0,
      eye.getEyeView(), 0, headMatrix, 0);
    Matrix.multiplyMM(mvpMatrix, 0,
      eye.getPerspective(0.1f, 100.0f), 0, viewMatrix, 0);
    mvpBuffer.put(mvpMatrix);
    mvpBuffer.rewind();
    GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER, 
      0, mvpMatrix.length * 4, mvpBuffer);

    // Draw the squares
    GLES32.glDrawElementsInstanced(GLES32.GL_TRIANGLE_STRIP, 4,
      GLES32.GL_UNSIGNED_BYTE, 0, 16);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  private void drawPointer() {

    // Set the program and VAO
    GLES32.glBindVertexArray(vaos[1]);
    GLES32.glUseProgram(programs[1]);

    // Draw the pointer circle
    GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 0, 16);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  @Override
  public void onDrawEye(Eye eye) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT |
      GLES32.GL_DEPTH_BUFFER_BIT);

    // Draw squares
    drawSquare(eye);

    // Draw pointer
    if(connected) {
      drawPointer();
    }
  }

  Handler getHandler() {
    return handler;
  }

  @Override
  public void onSurfaceChanged(int width, int height) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {
    GLES32.glDeleteBuffers(NUM_VBOS, vbos, 0);
    GLES32.glDeleteBuffers(NUM_IBOS, ibos, 0);
    GLES32.glDeleteBuffers(NUM_UBOS, ubos, 0);
    GLES32.glDeleteVertexArrays(NUM_VAOS, vaos, 0);
  }
}