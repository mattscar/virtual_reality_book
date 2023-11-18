package com.dreambookvr.ch08_fourtriangles;

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

class FourTrianglesRenderer
  implements GvrView.StereoRenderer {

  private static final String VERTEX_SHADER = "fourtriangles.vert";
  private static final String FRAGMENT_SHADER = "fourtriangles.frag";

  private Context context;
  private int coordIndex, colorIndex, rotMatrixLocation;
  private FloatBuffer vertexBuffer, matrixBuffer;

  // Vertex data
  private static final float[] vertexData = {
    -0.1f, -0.0866f, 1.0f, 0.0f, 0.0f,   // First vertex
     0.0f,  0.0866f, 0.0f, 1.0f, 0.0f,   // Second vertex
     0.1f, -0.0866f, 0.0f, 0.0f, 1.0f};  // Third vertex

  FourTrianglesRenderer(Context ctx) {
    context = ctx;

    // Initialize rotation matrix
    float[] rotMatrix = new float[16];
    Matrix.setRotateM(rotMatrix, 0, 15.0f, 0.0f, 0.0f, 1.0f);
    
    // Store attribute data in an NIO buffer
    ByteBuffer tempBuffer =
      ByteBuffer.allocateDirect(vertexData.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    vertexBuffer = tempBuffer.asFloatBuffer();
    vertexBuffer.put(vertexData);
    vertexBuffer.rewind();

    // Store uniform data in an NIO buffer
    tempBuffer =
        ByteBuffer.allocateDirect(rotMatrix.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    matrixBuffer = tempBuffer.asFloatBuffer();
    matrixBuffer.put(rotMatrix);
    matrixBuffer.rewind();
  }

  @Override  
  public void onSurfaceCreated(EGLConfig config) {

    // Set the region's background color and line width
    GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    GLES32.glLineWidth(10);

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

    // Obtain the location of the uniform variable
    coordIndex = GLES32.glGetAttribLocation(program, "in_coords");
    colorIndex = GLES32.glGetAttribLocation(program, "in_color");
    rotMatrixLocation =
      GLES32.glGetUniformLocation(program, "rot_matrix");
  }

  @Override
  public void onNewFrame(HeadTransform headTransform) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

    // Associate coordinate data with in_coords
    vertexBuffer.rewind();
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
        GLES32.GL_FLOAT, false, 20, vertexBuffer);

    // Associate color data with in_colors
    vertexBuffer.position(2);
    GLES32.glEnableVertexAttribArray(colorIndex);
    GLES32.glVertexAttribPointer(colorIndex, 3,
        GLES32.GL_FLOAT, false, 20, vertexBuffer);

    // Associate rotation matrix with rot_matrix
    GLES32.glUniformMatrix4fv(rotMatrixLocation, 1, false, matrixBuffer);

    // Draw four triangle instances
    GLES32.glDrawArraysInstanced(GLES32.GL_LINE_LOOP, 0, 3, 4);
  }

  @Override  
  public void onDrawEye(Eye eye) {}

  @Override
  public void onSurfaceChanged(int width, int height) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {}  
}