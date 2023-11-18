#include "spinningtexture_renderer.h"

static const char* VERTEX_SHADER = "spinningtexture.vert";
static const char* FRAGMENT_SHADER = "spinningtexture.frag";

// Near and far clipping planes.
static const float near = 1.0f;
static const float far = 100.0f;

// Vertex coordinates and texture coordinates
static const GLfloat vertexData[] = {
  -0.5f, -0.5f, -5.0f, 0.0f, 0.0f,
   0.5f, -0.5f, -5.0f, 1.0f, 0.0f,
   0.5f,  0.5f, -5.0f, 1.0f, 1.0f,
  -0.5f,  0.5f, -5.0f, 0.0f, 1.0f};

// Index data
static const GLubyte indexData[] = {1, 0, 2, 3};

SpinningTextureRenderer::SpinningTextureRenderer(
  AAssetManager* assetMgr, gvr_context* gvrContext):
  gvrApi(gvr::GvrApi::WrapNonOwned(gvrContext)),
  buffViewport(gvrApi->CreateBufferViewport()),
  assetManager(assetMgr),
  angle(0.0f),
  ready(false) {}

SpinningTextureRenderer::~SpinningTextureRenderer() {
  glDeleteBuffers(NUM_VBOS, vbos);
  glDeleteBuffers(NUM_IBOS, ibos);
  glDeleteBuffers(NUM_UBOS, ubos);
  glDeleteVertexArrays(NUM_VAOS, vaos);
  glDeleteTextures(NUM_TEXTURES, tids);
}

void SpinningTextureRenderer::InitShader() {

  // Read and compile vertex shader
  GLuint vertDescriptor = glCreateShader(GL_VERTEX_SHADER);
  std::string vertFile = ShaderUtils::ReadFile(assetManager, VERTEX_SHADER);
  const GLchar* vertSource = vertFile.c_str();
  glShaderSource(vertDescriptor, 1, &vertSource, 0);
  ShaderUtils::CompileShader(vertDescriptor);

  // Read and compile fragment shader
  GLuint fragDescriptor = glCreateShader(GL_FRAGMENT_SHADER);
  std::string fragFile = ShaderUtils::ReadFile(assetManager, FRAGMENT_SHADER);
  const GLchar* fragSource = fragFile.c_str();

  glShaderSource(fragDescriptor, 1, &fragSource, 0);
  ShaderUtils::CompileShader(fragDescriptor);

  // Create program and bind attributes
  program = glCreateProgram();
  glAttachShader(program, vertDescriptor);
  glAttachShader(program, fragDescriptor);

  // Link and use program
  ShaderUtils::LinkProgram(program);
  glUseProgram(program);
}

void SpinningTextureRenderer::InitBuffers() {

  // Generate VAOs and VBOs
  glGenVertexArrays(NUM_VAOS, vaos);
  glGenBuffers(NUM_VBOS, vbos);

  // Bind the VAO
  glBindVertexArray(vaos[0]);

  // Configure the VBO
  glBindBuffer(GL_ARRAY_BUFFER, vbos[0]);
  int dataSize = sizeof(vertexData);
  glBufferData(GL_ARRAY_BUFFER,
    dataSize, NULL, GL_DYNAMIC_DRAW);
  GLfloat* vertexBuffer = (GLfloat*)glMapBufferRange(
    GL_ARRAY_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(vertexData, vertexData + 20, vertexBuffer);
  glUnmapBuffer(GL_ARRAY_BUFFER);

  // Associate coordinate data with in_coords
  GLint coordIndex = glGetAttribLocation(program, "in_coords");
  glEnableVertexAttribArray((GLuint)coordIndex);
  glVertexAttribPointer((GLuint)coordIndex, 3,
    GL_FLOAT, GL_FALSE, 5*sizeof(GLfloat), 0);

  // Associate color data with in_texcoords
  GLint texcoordIndex = glGetAttribLocation(program, "in_texcoords");
  glEnableVertexAttribArray((GLuint)texcoordIndex);
  glVertexAttribPointer((GLuint)texcoordIndex, 2,
    GL_FLOAT, GL_FALSE, 5*sizeof(GLfloat), (GLvoid*)12);

  // Configure the IBO
  glGenBuffers(NUM_IBOS, ibos);
  dataSize = sizeof(indexData);
  glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibos[0]);
  glBufferData(GL_ELEMENT_ARRAY_BUFFER, dataSize, NULL, GL_DYNAMIC_DRAW);
  GLubyte* indexBuffer = (GLubyte*)glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER,
    0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(indexData, indexData + dataSize, indexBuffer);
  glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);

  // Unbind the VAO
  glBindVertexArray(0);

  // Configure the UBO
  glGenBuffers(NUM_UBOS, ubos);
  glBindBuffer(GL_UNIFORM_BUFFER, ubos[0]);
  glBufferData(GL_UNIFORM_BUFFER, sizeof(gvr::Mat4f), NULL, GL_DYNAMIC_DRAW);

  // Associate offset data with the ubo block
  GLuint bindingPoint = 1;
  GLuint uboIndex = glGetUniformBlockIndex(program, "ubo");
  glUniformBlockBinding(program, uboIndex, bindingPoint);
  glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, ubos[0]);
}

void SpinningTextureRenderer::InitTexture() {

  glActiveTexture(GL_TEXTURE0);
  glGenTextures(NUM_TEXTURES, tids);
  glBindTexture(GL_TEXTURE_2D, tids[0]);

  // Read pixel data and associate it with texture
  AAsset* asset = AAssetManager_open(assetManager, "v.bmp", AASSET_MODE_BUFFER);
  glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
    400, 400, 0, GL_RGB, GL_UNSIGNED_BYTE, AAsset_getBuffer(asset));

  // Set texture parameters
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

  glBindTexture(GL_TEXTURE_2D, 0);
  AAsset_close(asset);
}

void SpinningTextureRenderer::OnSurfaceCreated() {

  // Initialize OpenGL processing
  gvrApi->InitializeGl();
  InitShader();
  InitBuffers();
  InitTexture();
  glEnable(GL_SCISSOR_TEST);  
  
  // Determine the rendering size
  gvr::Sizei maxSize = gvrApi->GetMaximumEffectiveRenderTargetSize();
  renderSize.width = maxSize.width/2;
  renderSize.height = maxSize.height/2;

  // Define a BufferSpec
  std::vector<gvr::BufferSpec> specs;
  specs.push_back(gvrApi->CreateBufferSpec());
  specs[0].SetSize(renderSize);
  specs[0].SetColorFormat(GVR_COLOR_FORMAT_RGBA_8888);
  specs[0].SetDepthStencilFormat(GVR_DEPTH_STENCIL_FORMAT_DEPTH_16);
  specs[0].SetSamples(4);

  // Create the swap chain
  swapChain.reset(new gvr::SwapChain(gvrApi->CreateSwapChain(specs)));

  // Create the buffer viewport list
  viewports.reset(new gvr::BufferViewportList(
    gvrApi->CreateEmptyBufferViewportList()));

  ready = true;
}

void SpinningTextureRenderer::OnDrawFrame() {

  gvr::Sizei size = gvrApi->GetMaximumEffectiveRenderTargetSize();
  size.width = size.width/2;
  size.height = size.height/2;
  if(renderSize.width != size.width ||
     renderSize.height != size.height) {
    swapChain->ResizeBuffer(0, size);
    renderSize = size;
  }

  // Initialize the buffer viewport list
  viewports->SetToRecommendedBufferViewports();

  // Set the rotation
  angle += 0.1f;
  rotMatrix = MatrixUtils::RotateM(angle, 0.0f, 0.0f, -1.0f);

  // Determine the headset's future orientation
  gvr::ClockTimePoint time = gvr::GvrApi::GetTimePointNow();
  time.monotonic_system_time_nanos += 50000000;
  gvr::Mat4f initMatrix =
    gvrApi->GetHeadSpaceFromStartSpaceRotation(time);
  headMatrix = gvrApi->ApplyNeckModel(initMatrix, 1.0);

  // Acquire the frame and bind it
  gvr::Frame frame = swapChain->AcquireFrame();
  frame.BindBuffer(0);

  // Set VAO, texture, and program
  glBindVertexArray(vaos[0]);  
  glBindBuffer(GL_UNIFORM_BUFFER, ubos[0]);  
  glBindTexture(GL_TEXTURE_2D, tids[0]);
  
  // Set the clear color
  glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
  viewports->GetBufferViewport(0, &buffViewport);
  RenderEye(GVR_LEFT_EYE, buffViewport);
  viewports->GetBufferViewport(1, &buffViewport);
  RenderEye(GVR_RIGHT_EYE, buffViewport);

  glBindVertexArray(0);  
  
  // Unbind the frame
  frame.Unbind();

  // Submit the frame
  frame.Submit(*viewports, headMatrix);
}

void SpinningTextureRenderer::RenderEye(gvr::Eye eye,
  const gvr::BufferViewport& vport) {

  // Set the viewport and scissor
  const gvr::Rectf& rect = vport.GetSourceUv();
  int left = static_cast<int>(rect.left * renderSize.width);
  int bottom = static_cast<int>(rect.bottom * renderSize.height);
  int width = static_cast<int>((rect.right - rect.left) * renderSize.width);
  int height = static_cast<int>((rect.top - rect.bottom) * renderSize.height);
  glViewport(left, bottom, width, height);
  glScissor(left, bottom, width, height);  
  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

  // Compute the MVP matrix
  gvr::Mat4f viewMatrix = MatrixUtils::MultiplyMM(
    gvrApi->GetEyeFromHeadMatrix(eye), headMatrix);
  gvr::Mat4f projMatrix =
    MatrixUtils::Perspective(vport.GetSourceFov(), near, far);
  gvr::Mat4f mvpMatrix =
    MatrixUtils::MultiplyMM(projMatrix, viewMatrix);
  gvr::Mat4f lastMatrix =
      MatrixUtils::Transpose(MatrixUtils::MultiplyMM(mvpMatrix, rotMatrix));

  // Pass MVP matrix to shader
  glBufferSubData(GL_UNIFORM_BUFFER, 0,
    sizeof(gvr::Mat4f), (GLvoid*)lastMatrix.m);

  // Draw the square
  glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_BYTE, 0);
}

void SpinningTextureRenderer::OnPause() {
  if(ready) {
    gvrApi->PauseTracking();
  }
}

void SpinningTextureRenderer::OnResume() {
  if(ready) {
    gvrApi->ResumeTracking();
  }
}