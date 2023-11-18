package com.dreambookvr.ch11_rendertexture;

import android.content.Context;
import android.opengl.GLES32;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView.StereoRenderer;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;

class RenderTextureRenderer implements StereoRenderer {

  // Shaders
  private static final String TRIANGLE_VERTEX_SHADER = "triangle.vert";
  private static final String TRIANGLE_FRAGMENT_SHADER = "triangle.frag";
  private static final String RENDER_VERTEX_SHADER = "rendertexture.vert";
  private static final String RENDER_FRAGMENT_SHADER = "rendertexture.frag";

  // Object counts
  private static final int NUM_VAOS = 2;
  private static final int NUM_VBOS = 2;
  private static final int NUM_IBOS = 1;
  private static final int NUM_FBOS = 1;  
  private static final int NUM_TEXTURES = 1;    

  private Context context;
  private int[] programs, vaos, vbos, ibos, fbos, texIDs;

  // Triangle attribute data
  static private final float[] triangleData = {
    -0.5f, -0.5f, 1.0f, 0.0f, 0.0f,   // First vertex
     0.0f,  0.5f, 0.0f, 1.0f, 0.0f,   // Second vertex
     0.5f, -0.5f, 0.0f, 0.0f, 1.0f};  // Third vertex

  // Vertex coordinates and texture coordinates
  static private final float[] vertexData = {
    -0.5f, -0.5f, 0.0f, 0.0f,
     0.5f, -0.5f, 1.0f, 0.0f,
     0.5f,  0.5f, 1.0f, 1.0f,
    -0.5f,  0.5f, 0.0f, 1.0f};

  private static final byte[] indexData = {1, 0, 2, 3};

  RenderTextureRenderer(Context ctx) {
    context = ctx;

    // Initialize arrays
    programs = new int[2];
    texIDs = new int[NUM_TEXTURES];    
    vaos = new int[NUM_VAOS];
    vbos = new int[NUM_VBOS];
    ibos = new int[NUM_IBOS];
    fbos = new int[NUM_FBOS];
  }

  private void initShaders() {

    // Read and compile vertex shader
    int vertDescriptor = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
    String vertFile = ShaderUtils.readFile(context, TRIANGLE_VERTEX_SHADER);
    GLES32.glShaderSource(vertDescriptor, vertFile);
    ShaderUtils.compileShader(vertDescriptor);

    // Read and compile fragment shader
    int fragDescriptor = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
    String fragFile = ShaderUtils.readFile(context, TRIANGLE_FRAGMENT_SHADER);
    GLES32.glShaderSource(fragDescriptor, fragFile);
    ShaderUtils.compileShader(fragDescriptor);

    // Create and link first program
    programs[0] = GLES32.glCreateProgram();
    GLES32.glAttachShader(programs[0], vertDescriptor);
    GLES32.glAttachShader(programs[0], fragDescriptor);
    ShaderUtils.linkProgram(programs[0]);

    GLES32.glUseProgram(programs[0]);

    // Read and compile vertex shader
    vertDescriptor = GLES32.glCreateShader(GLES32.GL_VERTEX_SHADER);
    vertFile = ShaderUtils.readFile(context, RENDER_VERTEX_SHADER);
    GLES32.glShaderSource(vertDescriptor, vertFile);
    ShaderUtils.compileShader(vertDescriptor);

    // Read and compile fragment shader
    fragDescriptor = GLES32.glCreateShader(GLES32.GL_FRAGMENT_SHADER);
    fragFile = ShaderUtils.readFile(context, RENDER_FRAGMENT_SHADER);
    GLES32.glShaderSource(fragDescriptor, fragFile);
    ShaderUtils.compileShader(fragDescriptor);

    // Create first program
    programs[1] = GLES32.glCreateProgram();
    GLES32.glAttachShader(programs[1], vertDescriptor);
    GLES32.glAttachShader(programs[1], fragDescriptor);
    ShaderUtils.linkProgram(programs[1]);
  }

  private void initFramebuffer() {

    // Enable texture processing
    GLES32.glEnable(GLES32.GL_TEXTURE_2D);  

    // Make the first texture unit active
    GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
    
    // Generate framebuffer
    GLES32.glGenFramebuffers(1, fbos, 0);  
    GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, fbos[0]);
      
    // Generate a texture descriptor
    GLES32.glGenTextures(1, texIDs, 0);
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texIDs[0]);

    // Set texture parameters
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);

    // Read pixel data and associate it with texture
    GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGBA, 
      800, 800, 0, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, null);
    
    GLES32.glFramebufferTexture(GLES32.GL_FRAMEBUFFER,
      GLES32.GL_COLOR_ATTACHMENT0, texIDs[0], 0);

    // Identify the attachment point
    int[] attachmentPoints = new int[1];
    attachmentPoints[0] = GLES32.GL_COLOR_ATTACHMENT0;
    ByteBuffer tempBuffer =
      ByteBuffer.allocateDirect(attachmentPoints.length * 4);
    tempBuffer.order(ByteOrder.nativeOrder());
    IntBuffer pointsBuffer = tempBuffer.asIntBuffer();
    pointsBuffer.put(attachmentPoints);
    pointsBuffer.rewind();
    GLES32.glDrawBuffers(1, pointsBuffer);    
    
    GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);
  }

  private void initTriangleData() {

    // Bind the VAO
    GLES32.glBindVertexArray(vaos[0]);

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[0]);
    int dataSize = triangleData.length * 4;
    GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
      dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    ByteBuffer buff = (ByteBuffer)GLES32.glMapBufferRange(
      GLES32.GL_ARRAY_BUFFER, 0, dataSize,
      GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    FloatBuffer tmpBuffer = buff.asFloatBuffer();
    tmpBuffer.put(triangleData);
    GLES32.glUnmapBuffer(GLES32.GL_ARRAY_BUFFER);

    // Associate coordinate data with in_coords
    int coordIndex = GLES32.glGetAttribLocation(programs[0], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
      GLES32.GL_FLOAT, false, 20, 0);

    // Associate color data with in_colors
    int colorIndex = GLES32.glGetAttribLocation(programs[0], "in_color");
    GLES32.glEnableVertexAttribArray(colorIndex);
    GLES32.glVertexAttribPointer(colorIndex, 3,
      GLES32.GL_FLOAT, false, 20, 2 * 4);

    // Bind the VAO
    GLES32.glBindVertexArray(0);
  }

  private void initRenderData() {

    // Bind the VAO
    GLES32.glBindVertexArray(vaos[1]);

    // Configure the VBO
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, vbos[1]);
    int dataSize = vertexData.length * 4;
    GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,
      dataSize, null, GLES32.GL_DYNAMIC_DRAW);
    ByteBuffer buff = (ByteBuffer)GLES32.glMapBufferRange(
      GLES32.GL_ARRAY_BUFFER, 0, dataSize,
      GLES32.GL_MAP_WRITE_BIT | GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.order(ByteOrder.nativeOrder());
    FloatBuffer tmpBuffer = buff.asFloatBuffer();
    tmpBuffer.put(vertexData);
    GLES32.glUnmapBuffer(GLES32.GL_ARRAY_BUFFER);

    // Associate coordinate data with in_coords
    int coordIndex = GLES32.glGetAttribLocation(programs[1], "in_coords");
    GLES32.glEnableVertexAttribArray(coordIndex);
    GLES32.glVertexAttribPointer(coordIndex, 2,
      GLES32.GL_FLOAT, false, 16, 0);

    // Associate color data with in_texcoords
    int texcoordIndex = GLES32.glGetAttribLocation(programs[1], "in_texcoords");
    GLES32.glEnableVertexAttribArray(texcoordIndex);
    GLES32.glVertexAttribPointer(texcoordIndex, 2,
      GLES32.GL_FLOAT, false, 16, 2 * 4);

    // Configure the IBO
    GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, ibos[0]);
    GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER,
      indexData.length, null, GLES32.GL_DYNAMIC_DRAW);
    buff = (ByteBuffer)GLES32.glMapBufferRange(GLES32.GL_ELEMENT_ARRAY_BUFFER,
      0, indexData.length, GLES32.GL_MAP_WRITE_BIT |
      GLES32.GL_MAP_UNSYNCHRONIZED_BIT);
    buff.put(indexData);
    GLES32.glUnmapBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  private void renderToTexture() {

    // Set the FBO
    GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, fbos[0]);

    // Bind the VAO and program
    GLES32.glBindVertexArray(vaos[0]);
    GLES32.glUseProgram(programs[0]);

    // Configure draw operation
    GLES32.glViewport(0, 0, 800, 800);
    GLES32.glLineWidth(10);
    GLES32.glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

    // Draw the triangle
    GLES32.glDrawArrays(GLES32.GL_LINE_LOOP, 0, 3);

    // Unbind the FBO and VAO
    GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);
    GLES32.glBindVertexArray(0);
  }

  @Override
  public void onSurfaceCreated(EGLConfig config) {

    // Set the region's background color
    GLES32.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

    // Configure shader processing
    initShaders();

    // Configure framebuffer
    initFramebuffer();

    // Generate objects
    GLES32.glGenVertexArrays(NUM_VAOS, vaos, 0);
    GLES32.glGenBuffers(NUM_VBOS, vbos, 0);
    GLES32.glGenBuffers(NUM_IBOS, ibos, 0);

    // Configure data for triangle drawing and rendering
    initTriangleData();
    initRenderData();

    // Draw a triangle on the texture
    renderToTexture();
  }

  @Override
  public void onDrawEye(Eye eye) {

    // Fill the rendering region with the background color
    GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);

    // Bind the VAO and program
    GLES32.glBindVertexArray(vaos[1]);
    GLES32.glUseProgram(programs[1]);

    // Bind the first texture
    GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texIDs[0]);

    // Draw the rendertexture
    GLES32.glDrawElements(GLES32.GL_TRIANGLE_STRIP, 4,
        GLES32.GL_UNSIGNED_BYTE, 0);

    // Unbind the VAO
    GLES32.glBindVertexArray(0);
  }

  @Override
  public void onNewFrame(HeadTransform headTransform) {}

  @Override
  public void onSurfaceChanged(int width, int height) {}

  @Override
  public void onFinishFrame(Viewport viewport) {}

  @Override
  public void onRendererShutdown() {
    GLES32.glDeleteBuffers(NUM_VBOS, vbos, 0);
    GLES32.glDeleteBuffers(NUM_IBOS, ibos, 0);    
    GLES32.glDeleteVertexArrays(NUM_VAOS, vaos, 0);    
    GLES32.glDeleteFramebuffers(NUM_FBOS, fbos, 0);    
    GLES32.glDeleteTextures(NUM_TEXTURES, texIDs, 0);
  }
}
