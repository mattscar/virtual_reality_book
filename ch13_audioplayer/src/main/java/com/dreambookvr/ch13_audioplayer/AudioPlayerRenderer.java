package com.dreambookvr.ch13_audioplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES32;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView.StereoRenderer;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;

class AudioPlayerRenderer implements StereoRenderer {

  private static final float CIRCLE_RADIUS = 0.04f;
  private static final float DRAW_DEPTH = -5.0f;
  private static final int TEXT_HEIGHT = 32;
  private static final String AUDIO_FILE = "harp.wav";
  private static final String[] shaderNames =
    {"player.vert", "player.frag", "buttons.vert", "buttons.frag",
     "circle.vert", "circle.frag", "text.vert", "text.frag"};
  private static final String[] playerStates = {"Init", "Playing", "Paused", "Stopped"};

  // Object counts
  private static final int NUM_VAOS = 4;
  private static final int NUM_VBOS = 4;
  private static final int NUM_IBOS = 1;
  private static final int NUM_UBOS = 1;
  private static final int NUM_TEXTURES = 2;
  private static final int NUM_PROGRAMS = shaderNames.length/2;

  private final Context context;
  private int[] programs, vaos, vbos, ibos, tids, ubos;
  private float[] headMatrix, viewMatrix, mvpMatrix, target;
  private int sourceId, firstVertex = 0, selectedButton = -1;
  private final GvrAudioEngine engine;

  private FloatBuffer offsetBuffer, mvpBuffer, targetBuffer;
  private IntBuffer selectBuffer;
  static private float[] quat;
  static private boolean buttonDown, connected = false;
  static private Bitmap buttonBitmap, textBitmap;

  // Vertex coordinates and texture coordinates
  static private float playerVertices[] = {
    -1.0f, -0.6f, 0.65f, 0.65f, 0.65f,
     1.0f, -0.6f, 0.65f, 0.65f, 0.65f,
     1.0f,  0.5f, 0.65f, 0.65f, 0.65f,
    -1.0f,  0.5f, 0.65f, 0.65f, 0.65f,
    -0.55f,  0.1f, 1.0f, 1.0f, 1.0f,
     0.55f,  0.1f, 1.0f, 1.0f, 1.0f,
     0.55f,  0.35f, 1.0f, 1.0f, 1.0f,
    -0.55f,  0.35f, 1.0f, 1.0f, 1.0f};

  // Index data
  static private byte playerIndices[] =
    {1, 0, 2, 3, (byte)0xff, 5, 4, 6, 7};

  // Vertex coordinates and texture coordinates
  static private float buttonVertices[] = {
     0.2f, -0.2f, 0.33f, 0.5f,
    -0.2f, -0.2f, 0.01f, 0.5f,
     0.2f,  0.2f, 0.33f, 1.0f,
    -0.2f,  0.2f, 0.01f, 1.0f};

  // Button offsets
  static private float buttonOffsets[] = {
    -0.6f, -0.3f, 0.0f, -0.3f, 0.6f, -0.3f};

  // Text vertex data
  private static float[] textVertices = {
     0.2f,  0.12f,  1.0f,  0.25f,
    -0.5f,  0.12f,  0.0f,  0.25f,
     0.2f,  0.32f,  1.0f,  0.00f,
    -0.5f,  0.32f,  0.0f,  0.00f,
     0.2f,  0.12f,  1.0f,  0.50f,
    -0.5f,  0.12f,  0.0f,  0.50f,
     0.2f,  0.32f,  1.0f,  0.25f,
    -0.5f,  0.32f,  0.0f,  0.25f,
     0.2f,  0.12f,  1.0f,  0.75f,
    -0.5f,  0.12f,  0.0f,  0.75f,
     0.2f,  0.32f,  1.0f,  0.50f,
    -0.5f,  0.32f,  0.0f,  0.50f,
     0.2f,  0.12f,  1.0f,  1.00f,
    -0.5f,  0.12f,  0.0f,  1.00f,
     0.2f,  0.32f,  1.0f,  0.75f,
    -0.5f,  0.32f,  0.0f,  0.75f
  };

  // Display results from controller
  static private class ControllerHandler extends Handler {
    public void handleMessage(Message msg) {
      Bundle b = msg.getData();
      connected = b.getString("CONNECTION_MSG", "").equals("CONNECTED");
      buttonDown = b.getBoolean("APP_KEY_PRESSED");
      quat = b.getFloatArray("QUAT");
    }
  }
  private ControllerHandler handler = new ControllerHandler();

  AudioPlayerRenderer(Context ctx, GvrAudioEngine e) {
    context = ctx;
    engine = e;

    // Initialize objects and programs
    vaos = new int[NUM_VAOS];
    vbos = new int[NUM_VBOS];
    ibos = new int[NUM_IBOS];
    tids = new int[NUM_TEXTURES];
    ubos = new int[NUM_UBOS];
    programs = new int[NUM_PROGRAMS];

    // Initialize other structures
    quat = new float[4];    
    headMatrix = new float[16];
    viewMatrix = new float[16];
    mvpMatrix = new float[16];

    // Create Bitmap for buttons
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    buttonBitmap = BitmapFactory.decodeResource(
      ctx.getResources(), R.drawable.buttons, options);

    // Initialize audio playing
    engine.preloadSoundFile(AUDIO_FILE);
    sourceId = engine.createSoundObject(AUDIO_FILE);
    engine.setSoundObjectPosition(sourceId, 0.0f, 0.0f, 5.0f);
    engine.setSoundVolume(sourceId, 0.5f);

    // Create buffer for matrix data
    ByteBuffer tempBuffer = ByteBuffer.allocateDirect(mvpMatrix.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    mvpBuffer = tempBuffer.asFloatBuffer();
    mvpBuffer.rewind();

    // Store offset data in buffer
    tempBuffer = ByteBuffer.allocateDirect(buttonOffsets.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    offsetBuffer = tempBuffer.asFloatBuffer();
    offsetBuffer.put(buttonOffsets);
    offsetBuffer.rewind();

    // Create buffer to store target
    target = new float[2];
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

    // Initialize text canvas
    initTextCanvas();
  }

  private void initTextCanvas() {

    // Create Paint object
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.setAntiAlias(true);
    paint.setTextSize(TEXT_HEIGHT);

    // Determine text width
    int tmp, textWidth = 0;
    for(String state: playerStates) {
      tmp = java.lang.Math.round(paint.measureText(state));
      if(tmp > textWidth) {
        textWidth = tmp;
      }
    }

    // Create Bitmap for Canvas text
    int canvasHeight = (TEXT_HEIGHT + 6) * playerStates.length;
    textBitmap = Bitmap.createBitmap(textWidth, canvasHeight + 4, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(textBitmap);
    canvas.drawARGB(255, 255, 255, 255);

    // Draw text on canvas
    float height = 0.0f;
    for(String state: playerStates) {
      height += TEXT_HEIGHT;
      canvas.drawText(state, 0.0f, height, paint);
      height += 6.0f;
    }
  }

  private void initShaders() {

    int vertDescriptor, fragDescriptor;
    String vertFile, fragFile;
    int progIndex;

    // Process each pair of shaders
    for(int i=0; i<shaderNames.length; i+=2) {

      // Read and compile vertex shader
      vertDescriptor = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
      vertFile = ShaderUtils.readFile(context, shaderNames[i]);
      GLES32.glShaderSource(vertDescriptor, vertFile);
      ShaderUtils.compileShader(vertDescriptor);

      // Read and compile fragment shader
      fragDescriptor = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
      fragFile = ShaderUtils.readFile(context, shaderNames[i+1]);
      GLES32.glShaderSource(fragDescriptor, fragFile);
      ShaderUtils.compileShader(fragDescriptor);

      // Link and use program
      progIndex = i/2;
      programs[progIndex] = GLES32.glCreateProgram();
      GLES32.glAttachShader(programs[progIndex], vertDescriptor);
      GLES32.glAttachShader(programs[progIndex], fragDescriptor);
      ShaderUtils.linkProgram(programs[progIndex]);
      GLES32.glUseProgram(programs[progIndex]);
    }
  }

  private void initTextures() {

    // Make the first texture unit active
    GLES32.glActiveTexture(GLES32.GL_TEXTURE0);

    // Bind the first texture descriptor to its target
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tids[0]);

    // Set texture parameters
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);

    // Read pixel data and associate it with texture
    GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, buttonBitmap, 0);

    // Bind the second texture descriptor to its target
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tids[1]);

    // Set texture parameters
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);

    // Read pixel data and associate it with texture
    GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, textBitmap, 0);
  }

  private void initPlayer() {

    // Bind the VAO
    GLES32.glBindVertexArray(vaos[0]);

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[0]);
    int dataSize = playerVertices.length * 4;
    GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
      dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    ByteBuffer buff = (ByteBuffer)GLES32.glMapBufferRange(
      GLES32.GL_ARRAY_BUFFER, 0, dataSize,
      GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    buff.asFloatBuffer().put(playerVertices);
    GLES32.glUnmapBuffer(GLES32.GL_ARRAY_BUFFER);

    // Associate coordinate data with in_coords
    int coordIndex = GLES32.glGetAttribLocation(programs[0], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
      GLES32.GL_FLOAT, false, 20, 0);

    // Associate coordinate data with in_coords
    int colorIndex = GLES32.glGetAttribLocation(programs[0], "in_color");
    GLES32.glEnableVertexAttribArray(colorIndex);
    GLES32.glVertexAttribPointer(colorIndex, 3,
      GLES32.GL_FLOAT, false, 20, 8);

    // Configure the IBO
    dataSize = playerIndices.length;
    GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, ibos[0]);
    GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER,
      dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    buff = (ByteBuffer)GLES32.glMapBufferRange(
      GLES32.GL_ELEMENT_ARRAY_BUFFER, 0, dataSize,
      GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    buff.put(playerIndices);
    GLES32.glUnmapBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  private void initButtons() {

    // Set the VAO
    GLES32.glBindVertexArray(vaos[1]);

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[1]);
    int dataSize = buttonVertices.length * 4;
    GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
      dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    ByteBuffer buff = (ByteBuffer)GLES32.glMapBufferRange(
      GLES32.GL_ARRAY_BUFFER, 0, dataSize,
      GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    buff.asFloatBuffer().put(buttonVertices);
    GLES32.glUnmapBuffer(GLES32.GL_ARRAY_BUFFER);

    // Associate coordinate data with in_coords
    int coordIndex = GLES32.glGetAttribLocation(programs[1], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
      GLES32.GL_FLOAT, false, 16, 0);

    // Associate coordinate data with in_coords
    int texcoordIndex = GLES32.glGetAttribLocation(programs[1], "in_texcoords");
    GLES32.glEnableVertexAttribArray(texcoordIndex);
    GLES32.glVertexAttribPointer(texcoordIndex, 2,
      GLES32.GL_FLOAT, false, 16, 8);

    // Store offsets to the UBO
    GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,
      (mvpMatrix.length + target.length) * 4,
      buttonOffsets.length * 4, offsetBuffer);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  private void initPointer() {

    // Set the program and VBO
    GLES32.glBindVertexArray(vaos[2]);

    // Initialize circle vertices
    float circleVertices[] = new float[32];
    circleVertices[0] = 0.0f;
    circleVertices[1] = 0.0f;
    for(int i=0; i<15; i++) {
      circleVertices[2*(i+1)] = CIRCLE_RADIUS * (float)Math.cos((2.0f * Math.PI * i)/14);
      circleVertices[2*(i+1)+1] = CIRCLE_RADIUS * (float)Math.sin((2.0f * Math.PI * i)/14);
    }

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[2]);
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
    int coordIndex = GLES32.glGetAttribLocation(programs[2], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
      GLES32.GL_FLOAT, false, 0, 0);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  private void initText() {

    // Set the VAO
    GLES32.glBindVertexArray(vaos[3]);

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[3]);
    int dataSize = textVertices.length * 4;
    GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
      dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    ByteBuffer buff = (ByteBuffer)GLES32.glMapBufferRange(
      GLES32.GL_ARRAY_BUFFER, 0, dataSize,
      GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    buff.asFloatBuffer().put(textVertices);
    GLES32.glUnmapBuffer(GLES32.GL_ARRAY_BUFFER);

    // Associate coordinate data with in_coords
    int coordIndex = GLES32.glGetAttribLocation(programs[3], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
      GLES32.GL_FLOAT, false, 16, 0);

    // Associate coordinate data with in_coords
    int texcoordIndex = GLES32.glGetAttribLocation(programs[3], "in_texcoords");
    GLES32.glEnableVertexAttribArray(texcoordIndex);
    GLES32.glVertexAttribPointer(texcoordIndex, 2,
      GLES32.GL_FLOAT, false, 16, 8);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  @Override
  public void onSurfaceCreated(EGLConfig config) {

    // Set the region's background color and line width
    GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

    // Enable primitive restart and texture processing
    GLES32.glEnable(GLES32.GL_PRIMITIVE_RESTART_FIXED_INDEX);

    // Generate objects and shaders
    GLES32.glGenVertexArrays(NUM_VAOS, vaos, 0);
    GLES32.glGenBuffers(NUM_IBOS, ibos, 0);
    GLES32.glGenBuffers(NUM_VBOS, vbos, 0);
    GLES32.glGenBuffers(NUM_UBOS, ubos, 0);
    GLES32.glGenTextures(NUM_TEXTURES, tids, 0);

    // Initialize shaders and textures
    initShaders();
    initTextures();

    // Initialize UBO
    GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER, ubos[0]);
    GLES32.glBufferData(GLES32.GL_UNIFORM_BUFFER,
      (mvpMatrix.length + target.length + buttonOffsets.length + 1) * 4,
      null, GLES32.GL_DYNAMIC_DRAW);

    // Associate each program with the UBO
    int uboIndex;
    for(int i=0; i<NUM_PROGRAMS; ++i) {
      uboIndex = GLES32.glGetUniformBlockIndex(programs[i], "ubo");
      GLES32.glUniformBlockBinding(programs[i], uboIndex, i);
      GLES32.glBindBufferBase(GLES32.GL_UNIFORM_BUFFER, i, ubos[0]);
    }

    // Initialize graphics
    initPlayer();
    initButtons();
    initPointer();
    initText();
  }

  @Override
public void onNewFrame(HeadTransform headTransform) {

  // Make the first texture unit active
  GLES32.glActiveTexture(GLES32.GL_TEXTURE0);

  // Determine where the controller is pointing
  float tmp = (quat[0] * quat[0]) - (quat[1] * quat[1]) - (quat[2] * quat[2]) + (quat[3] * quat[3]);
  target[0] = (2.0f * (quat[0] * quat[2]) - (2 * quat[1] * quat[3])) * (DRAW_DEPTH / tmp);
  target[1] = (-2.0f * (quat[0] * quat[1]) - (2 * quat[2] * quat[3])) * (DRAW_DEPTH / tmp);

  // Update the UBO with the target information
  targetBuffer.put(target);
  targetBuffer.rewind();
  GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER, ubos[0]);
  GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,
      mvpMatrix.length * 4, target.length * 4, targetBuffer);

  // Determine which square is selected
  boolean changed = false;
  int oldSelectedButton = selectedButton;
  if(buttonDown) {

    for(int i=0; i < buttonOffsets.length; i+=2) {
      if((target[0] > (buttonOffsets[i] - 0.2f)) &&
        (target[0] < (buttonOffsets[i] + 0.2f)) &&
        (target[1] > (buttonOffsets[i+1] - 0.2f)) &&
        (target[1] < (buttonOffsets[i+1] + 0.2f))) {
          if(selectedButton != i/2) {
            selectedButton = i/2;
            changed = true;
          }
          break;
      }
    }
  }

  // Update audio state
  if(changed) {

    // Process sound according to the selected button
    switch (selectedButton) {

      // Pause button
      case 0:
        if (engine.isSoundPlaying(sourceId)) {
          engine.pauseSound(sourceId);
        }
        firstVertex = 8;
        break;

      // Play button
      case 1:
        if((oldSelectedButton == 0) ||
            (oldSelectedButton == 2)) {
          engine.resumeSound(sourceId);

        }
        else {
          engine.playSound(sourceId, true);
        }
        firstVertex = 4;
        break;

      // Stop button
      case 2:
        engine.pauseSound(sourceId);
        firstVertex = 12;
        break;

      default:
        break;
    }
  }

  // Update the UBO with the selected square
  selectBuffer.put(selectedButton);
  selectBuffer.rewind();
  GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,
      (mvpMatrix.length + target.length + buttonOffsets.length) * 4, 4, selectBuffer);

  // Initialize the view matrix
  Matrix.setLookAtM(headMatrix, 0, 0.0f, 0.0f, 0.01f,
      0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

  // Update the audio engine
  float[] headQuat = new float[4];
  headTransform.getQuaternion(headQuat, 0);
  engine.setHeadRotation(headQuat[0], headQuat[1],
    headQuat[2], headQuat[3]);
  engine.update();
}

  @Override
  public void onDrawEye(Eye eye) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

    // Set model-view-projection matrix
    Matrix.multiplyMM(viewMatrix, 0,
      eye.getEyeView(), 0, headMatrix, 0);
    Matrix.multiplyMM(mvpMatrix, 0,
      eye.getPerspective(0.1f, 100.0f), 0, viewMatrix, 0);
    mvpBuffer.put(mvpMatrix);
    mvpBuffer.rewind();
    GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,
      0, mvpMatrix.length * 4, mvpBuffer);

    // Draw player
    GLES32.glUseProgram(programs[0]);
    GLES32.glBindVertexArray(vaos[0]);
    GLES32.glDrawElements(GLES32.GL_TRIANGLE_STRIP, 9, GLES32.GL_UNSIGNED_BYTE, 0);
    GLES32.glBindVertexArray(0);

    // Draw buttons
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tids[0]);
    GLES32.glUseProgram(programs[1]);
    GLES32.glBindVertexArray(vaos[1]);
    GLES32.glDrawArraysInstanced(GLES32.GL_TRIANGLE_STRIP, 0, 4, 3);
    GLES32.glBindVertexArray(0);
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

    // Draw text
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, tids[1]);
    GLES32.glUseProgram(programs[3]);
    GLES32.glBindVertexArray(vaos[3]);
    GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, firstVertex, 4);
    GLES32.glBindVertexArray(0);
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);

    // Draw pointer if connected
    if(connected) {
      GLES32.glUseProgram(programs[2]);
      GLES32.glBindVertexArray(vaos[2]);
      GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 0, 16);
      GLES32.glBindVertexArray(0);
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
