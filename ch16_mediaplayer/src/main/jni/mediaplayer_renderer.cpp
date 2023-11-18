#include "mediaplayer_renderer.h"

static const char* TAG = "MediaPlayer";
std::vector<std::string> shaderNames =  {"player.vert", "player.frag",
  "buttons.vert", "buttons.frag", "circle.vert", "circle.frag",
  "text.vert", "text.frag"};
static const float PLAYER_DEPTH = -5.0f;
static const float CIRCLE_RADIUS = 0.04f;
static std::string words[4] = {"Init", "Playing", "Paused", "Stopped"};

// Near and far clipping planes.
static const float near = 1.0f;
static const float far = 100.0f;

// Vertex coordinates and texture coordinates
static const GLfloat playerVertices[] = {
  -1.0f, -0.6f, 0.65f, 0.65f, 0.65f,
   1.0f, -0.6f, 0.65f, 0.65f, 0.65f,
   1.0f,  0.5f, 0.65f, 0.65f, 0.65f,
  -1.0f,  0.5f, 0.65f, 0.65f, 0.65f,
  -0.55f,  0.1f, 1.0f, 1.0f, 1.0f,
   0.55f,  0.1f, 1.0f, 1.0f, 1.0f,
   0.55f,  0.35f, 1.0f, 1.0f, 1.0f,
  -0.55f,  0.35f, 1.0f, 1.0f, 1.0f};

// Offsets
static const float offsets[] = {
  -0.6f, -0.3f, 0.0f, -0.3f, 0.6f, -0.3f};

// Vertex coordinates and texture coordinates
static const GLfloat buttonVertices[] = {
   0.2f, -0.2f, 0.33f, 0.5f,
  -0.2f, -0.2f, 0.01f, 0.5f,
   0.2f,  0.2f, 0.33f, 1.0f,
  -0.2f,  0.2f, 0.01f, 1.0f};

// Index data
static const GLubyte playerIndices[] =
  {1, 0, 2, 3, 255, 5, 4, 6, 7};

void handleMessage(GLenum source​, GLenum type​, GLuint id​,
  GLenum severity​, GLsizei length​, const GLchar* msg,
  const void* userData);

MediaPlayerRenderer::MediaPlayerRenderer(
  gvr_context* gvrContext,
  AAssetManager* assetMgr,
  std::unique_ptr<gvr::AudioApi> audioInst):
  gvrApi(gvr::GvrApi::WrapNonOwned(gvrContext)),
  audioApi(std::move(audioInst)),
  buffViewport(gvrApi->CreateBufferViewport()),
  assetManager(assetMgr),
  ready(false),
  firstFrame(true),
  selectedButton(-1),
  firstIndex(0),
  numIndices(4){}

MediaPlayerRenderer::~MediaPlayerRenderer() {
  glDeleteBuffers(NUM_VBOS, vbos);
  glDeleteBuffers(NUM_IBOS, ibos);
  glDeleteBuffers(NUM_UBOS, ubos);
  glDeleteVertexArrays(NUM_VAOS, vaos);
  glDeleteTextures(NUM_TEXTURES, tids);
}

void MediaPlayerRenderer::InitShaders() {

  GLuint vertDescriptor, fragDescriptor;
  std::string vertFile, fragFile;
  unsigned int progIndex;

  // Process each pair of shaders
  for(unsigned int i=0; i<shaderNames.size(); i+=2) {

    // Read and compile vertex shader
    vertDescriptor = glCreateShader(GL_VERTEX_SHADER);
    vertFile = ShaderUtils::ReadFile(assetManager, shaderNames[i].c_str());
    const GLchar* vertSource = vertFile.c_str();
    glShaderSource(vertDescriptor, 1, &vertSource, 0);
    ShaderUtils::CompileShader(vertDescriptor);

    // Read and compile fragment shader
    fragDescriptor = glCreateShader(GL_FRAGMENT_SHADER);
    fragFile = ShaderUtils::ReadFile(assetManager, shaderNames[i+1].c_str());
    const GLchar* fragSource = fragFile.c_str();
    glShaderSource(fragDescriptor, 1, &fragSource, 0);
    ShaderUtils::CompileShader(fragDescriptor);

    // Create program and bind attributes
    progIndex = i/2;
    programs[progIndex] = glCreateProgram();
    glAttachShader(programs[progIndex], vertDescriptor);
    glAttachShader(programs[progIndex], fragDescriptor);
    ShaderUtils::LinkProgram(programs[progIndex]);
  }
}

void MediaPlayerRenderer::InitPlayer() {

  // Bind the VAO
  glBindVertexArray(vaos[0]);

  // Configure the VBO
  glBindBuffer(GL_ARRAY_BUFFER, vbos[0]);
  GLsizeiptr dataSize = sizeof(playerVertices);
  int length = dataSize/sizeof(playerVertices[0]);
  glBufferStorageEXT(GL_ARRAY_BUFFER, dataSize, NULL,
    GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT_EXT | GL_MAP_COHERENT_BIT_EXT);
  GLfloat* vertexBuffer = (GLfloat*)glMapBufferRange(
    GL_ARRAY_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(playerVertices, playerVertices + length, vertexBuffer);
  glUnmapBuffer(GL_ARRAY_BUFFER);

  // Associate coordinate data with in_coords
  GLint coordIndex = glGetAttribLocation(programs[0], "in_coords");
  glEnableVertexAttribArray((GLuint)coordIndex);
  glVertexAttribPointer((GLuint)coordIndex, 2,
    GL_FLOAT, GL_FALSE, 5*sizeof(GLfloat), 0);

  // Associate color data with in_texcoords
  GLint colorIndex = glGetAttribLocation(programs[0], "in_color");
  glEnableVertexAttribArray((GLuint)colorIndex);
  glVertexAttribPointer((GLuint)colorIndex, 3,
    GL_FLOAT, GL_FALSE, 5*sizeof(GLfloat), (GLvoid*)8);

  // Configure the IBO
  dataSize = sizeof(playerIndices);
  glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibos[0]);
  glBufferData(GL_ELEMENT_ARRAY_BUFFER, dataSize, NULL, GL_DYNAMIC_DRAW);
  GLubyte* indexBuffer = (GLubyte*)glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER,
    0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(playerIndices, playerIndices + dataSize, indexBuffer);
  glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);

  // Unbind the VAO
  glBindVertexArray(0);
}

void MediaPlayerRenderer::InitButtons() {

  // Bind the VAO
  glBindVertexArray(vaos[1]);

  // Configure the VBO
  glBindBuffer(GL_ARRAY_BUFFER, vbos[1]);
  GLsizeiptr dataSize = sizeof(buttonVertices);
  int length = dataSize/sizeof(buttonVertices[0]);

  glBufferStorageEXT(GL_ARRAY_BUFFER, dataSize, NULL,
    GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT_EXT | GL_MAP_COHERENT_BIT_EXT);
  GLfloat* vertexBuffer = (GLfloat*)glMapBufferRange(
      GL_ARRAY_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(buttonVertices, buttonVertices + length, vertexBuffer);
  glUnmapBuffer(GL_ARRAY_BUFFER);

  // Associate coordinate data with in_coords
  GLint coordIndex = glGetAttribLocation(programs[1], "in_coords");
  glEnableVertexAttribArray((GLuint)coordIndex);
  glVertexAttribPointer((GLuint)coordIndex, 2,
    GL_FLOAT, GL_FALSE, 4*sizeof(GLfloat), 0);

  // Associate color data with in_texcoords
  GLint texcoordIndex = glGetAttribLocation(programs[1], "in_texcoords");
  glEnableVertexAttribArray((GLuint)texcoordIndex);
  glVertexAttribPointer((GLuint)texcoordIndex, 2,
    GL_FLOAT, GL_FALSE, 4*sizeof(GLfloat), (GLvoid*)8);

  // Unbind the VAO
  glBindVertexArray(0);
}

void MediaPlayerRenderer::InitPointer() {

  // Set the program and VBO
  glBindVertexArray(vaos[2]);

  // Initialize circle vertices
  float circleVertices[32];
  circleVertices[0] = 0.0f;
  circleVertices[1] = 0.0f;
  for(int i=0; i<15; i++) {
    circleVertices[2*(i+1)] = CIRCLE_RADIUS * (float)cos((2.0f * M_PI * i)/14);
    circleVertices[2*(i+1)+1] = CIRCLE_RADIUS * (float)sin((2.0f * M_PI * i)/14);
  }

  // Configure the VBO
  glBindBuffer(GL_ARRAY_BUFFER, vbos[2]);
  GLsizeiptr dataSize = sizeof(circleVertices);
  int length = dataSize/sizeof(circleVertices[0]);
  glBufferStorageEXT(GL_ARRAY_BUFFER, dataSize, NULL,
    GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT_EXT | GL_MAP_COHERENT_BIT_EXT);
  GLfloat* vertexBuffer = (GLfloat*)glMapBufferRange(
    GL_ARRAY_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(circleVertices, circleVertices + length, vertexBuffer);
  glUnmapBuffer(GL_ARRAY_BUFFER);

  // Associate coordinate data with in_coords
  GLint coordIndex = glGetAttribLocation(programs[2], "in_coords");
  glEnableVertexAttribArray((GLuint)coordIndex);
  glVertexAttribPointer((GLuint)coordIndex, 2,
    GL_FLOAT, GL_FALSE, 0, 0);

  // Set the selected button to -1
  glBufferSubData(GL_UNIFORM_BUFFER, sizeof(gvr::Mat4f) + 8*sizeof(float),
    sizeof(selectedButton), (GLvoid*)&selectedButton);

  // Unbind the VAO
  glBindVertexArray(0);
}

void MediaPlayerRenderer::InitTextures() {

  glActiveTexture(GL_TEXTURE0);
  glBindTexture(GL_TEXTURE_2D, tids[0]);

  // Read pixel data and associate it with texture
  AAsset* asset = AAssetManager_open(assetManager, "player.astc", AASSET_MODE_BUFFER);

  // Create a texture from the compressed data
  glCompressedTexImage2D(GL_TEXTURE_2D, 0,
    GL_COMPRESSED_RGBA_ASTC_4x4_KHR, 300, 200,
    0, 60000, AAsset_getBuffer(asset));

  // Set texture parameters
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
  glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

  // Close asset file
  AAsset_close(asset);

  // Create texture atlas to hold characters
  glBindTexture(GL_TEXTURE_2D, tids[1]);
  glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
  atlas = FreeTypeUtils::CreateAtlas(assetManager, "OpenSans-Regular.ttf", 32);

  glBindTexture(GL_TEXTURE_2D, 0);
}

void MediaPlayerRenderer::InitText() {

  TextureChar ch;
  int len, count = 0, numChars = 0;
  float x, texWidth, texAdvance, texHorizOffset, texLowOffset, texHighOffset;

  // Set dimensions
  float yLow = 0.1f;
  float yHigh = 0.3f;
  float scale = (yHigh - yLow) / atlas.lineHeight;

  // Determine the number of characters
  for(std::string word: words) {
    numChars += word.length();
  }

  // Set vertex indices
  GLfloat textVertices[16 * numChars];
  for(std::string word: words) {
    x = -0.5f;
    len = word.length();

    // Set vertices
    for (int i = 0; i < len; i++) {

      ch = atlas.charMap[word[i]];
      texWidth = ch.width * scale;
      texAdvance = ch.xadvance * scale;
      texHorizOffset = ch.xoffset * scale;
      texLowOffset = ch.yoffsetLow * scale;
      texHighOffset = ch.yoffsetHigh * scale;
      textVertices[count + 16 * i] = x + texHorizOffset + texWidth;
      textVertices[count + 16 * i + 1] = yLow + texLowOffset;
      textVertices[count + 16 * i + 2] = (float) (ch.x + ch.width) / atlas.textureWidth;
      textVertices[count + 16 * i + 3] = (float) ch.height / atlas.lineHeight;
      textVertices[count + 16 * i + 4] = x + texHorizOffset;
      textVertices[count + 16 * i + 5] = yLow + texLowOffset;
      textVertices[count + 16 * i + 6] = (float) ch.x / atlas.textureWidth;
      textVertices[count + 16 * i + 7] = (float) ch.height / atlas.lineHeight;
      textVertices[count + 16 * i + 8] = x + texHorizOffset + texWidth;
      textVertices[count + 16 * i + 9] = yHigh - texHighOffset;
      textVertices[count + 16 * i + 10] = (float) (ch.x + ch.width) / atlas.textureWidth;
      textVertices[count + 16 * i + 11] = 0.0f;
      textVertices[count + 16 * i + 12] = x + texHorizOffset;
      textVertices[count + 16 * i + 13] = yHigh - texHighOffset;
      textVertices[count + 16 * i + 14] = (float) ch.x / atlas.textureWidth;
      textVertices[count + 16 * i + 15] = 0.0f;
      x += texAdvance;
    }
    count += 16 * len;
  }
  
  // Set indices
  GLubyte textIndices[5 * numChars];
  for(unsigned int i=0; i<numChars; i++) {
    textIndices[5*i] = (GLubyte)(4*i);
    textIndices[5*i+1] = (GLubyte)(4*i+1);
    textIndices[5*i+2] = (GLubyte)(4*i+2);
    textIndices[5*i+3] = (GLubyte)(4*i+3);
    textIndices[5*i+4] = 0xff;
  }

  // Configure the VBO
  glBindVertexArray(vaos[3]);
  glBindBuffer(GL_ARRAY_BUFFER, vbos[3]);
  GLsizeiptr dataSize = sizeof(textVertices);
  len = dataSize/sizeof(textVertices[0]);
  glBufferStorageEXT(GL_ARRAY_BUFFER, dataSize, NULL,
    GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT_EXT | GL_MAP_COHERENT_BIT_EXT);
  GLfloat* vertexBuffer = (GLfloat*)glMapBufferRange(
    GL_ARRAY_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(textVertices, textVertices + len, vertexBuffer);
  glUnmapBuffer(GL_ARRAY_BUFFER);

  // Associate coordinate data with in_coords
  GLint coordIndex = glGetAttribLocation(programs[3], "in_coords");
  glEnableVertexAttribArray((GLuint)coordIndex);
  glVertexAttribPointer((GLuint)coordIndex, 2,
    GL_FLOAT, GL_FALSE, 4*sizeof(GLfloat), 0);

  // Associate color data with in_texcoords
  GLint texcoordIndex = glGetAttribLocation(programs[3], "in_texcoords");
  glEnableVertexAttribArray((GLuint)texcoordIndex);
  glVertexAttribPointer((GLuint)texcoordIndex, 2,
    GL_FLOAT, GL_FALSE, 4*sizeof(GLfloat), (GLvoid*)8);

  // Configure the IBO
  dataSize = sizeof(textIndices);
  glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibos[1]);
  glBufferData(GL_ELEMENT_ARRAY_BUFFER, dataSize, NULL, GL_DYNAMIC_DRAW);
  GLubyte* indexBuffer =
    (GLubyte*)glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, dataSize, GL_MAP_WRITE_BIT);
  std::copy(textIndices, textIndices + dataSize, indexBuffer);
  glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);

  // Unbind the VAO
  glBindVertexArray(0);
}

void MediaPlayerRenderer::OnSurfaceCreated() {

  // Initialize OpenGL processing
  gvrApi->InitializeGl();
  glEnable(GL_SCISSOR_TEST);
  glEnable(GL_PRIMITIVE_RESTART_FIXED_INDEX);
  glBufferStorageEXT =
    (PFNGLBUFFERSTORAGEEXTPROC) eglGetProcAddress("glBufferStorageEXT");

  // Configure event handling
  glEnable(GL_DEBUG_OUTPUT_KHR);
  glDebugMessageCallbackKHR(handleMessage, NULL);

  // Initialize objects and shaders
  glGenVertexArrays(NUM_VAOS, vaos);
  glGenBuffers(NUM_IBOS, ibos);
  glGenBuffers(NUM_VBOS, vbos);
  glGenBuffers(NUM_UBOS, ubos);
  glGenTextures(NUM_TEXTURES, tids);
  InitShaders();

  // Initialize uniform buffer object
  glBindBuffer(GL_UNIFORM_BUFFER, ubos[0]);
  glBufferData(GL_UNIFORM_BUFFER,
    sizeof(gvr::Mat4f) + sizeof(offsets) + 3*sizeof(int),
    NULL, GL_DYNAMIC_DRAW);
  glBufferSubData(GL_UNIFORM_BUFFER, sizeof(gvr::Mat4f) + 2*sizeof(int),
    sizeof(offsets), (GLvoid*)offsets);

  // Associate each program with the UBO
  GLuint uboIndex;
  for(GLuint i=0; i<NUM_PROGRAMS; ++i) {
    uboIndex = glGetUniformBlockIndex(programs[i], "ubo");
    glUniformBlockBinding(programs[i], uboIndex, i);
    glBindBufferBase(GL_UNIFORM_BUFFER, i, ubos[0]);
  }

  // Initialize data
  InitPlayer();
  InitButtons();
  InitPointer();
  InitTextures();
  InitText();

  // Initialize audio playing
  audioApi->PreloadSoundfile("harp.wav");
  id = audioApi->CreateSoundObject("harp.wav");
  audioApi->SetSoundObjectPosition(id, 0.0f, 0.0f, 5.0f);
  audioApi->SetSoundVolume(id, 0.5f);

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

  // Create the swap chain and viewport list
  swapChain.reset(new gvr::SwapChain(gvrApi->CreateSwapChain(specs)));
  viewports.reset(new gvr::BufferViewportList(
    gvrApi->CreateEmptyBufferViewportList()));

  // Initialize controller processing
  controllerApi.reset(new gvr::ControllerApi);
  controllerApi->Init(GVR_CONTROLLER_ENABLE_ORIENTATION);
  controllerApi->Resume();
  target[0] = 0.0f; target[1] = 0.0f;
  ready = true;
}

void MediaPlayerRenderer::OnDrawFrame() {

  glActiveTexture(GL_TEXTURE0);
  
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

  // Determine the headset's future orientation
  gvr::ClockTimePoint time = gvr::GvrApi::GetTimePointNow();
  time.monotonic_system_time_nanos += 50000000;
  gvr::Mat4f initMatrix =
    gvrApi->GetHeadSpaceFromStartSpaceRotation(time);
  headMatrix = gvrApi->ApplyNeckModel(initMatrix, 1.0);

  // Update audio processing
  audioApi->SetHeadPose(headMatrix);
  audioApi->Update();

  // Determine the controller's target if connected
  controllerState.Update(*controllerApi);
  if(!firstFrame && controllerState.GetConnectionState() == GVR_CONTROLLER_CONNECTED) {
    gvr_quatf q = controllerState.GetOrientation();
    float tmp = (q.qw * q.qw) - (q.qx * q.qx) - (q.qy * q.qy) + (q.qz * q.qz);
    target[0] = (2.0f * q.qw * q.qy - 2.0f * q.qx * q.qz) * (PLAYER_DEPTH / tmp);
    target[1] = (-2.0f * q.qw * q.qx - 2.0f * q.qy * q.qz) * (PLAYER_DEPTH / tmp);
  } else {
    target[0] = 0.0f; target[1] = 0.0f;
  }
  if(controllerState.GetRecentered()) {
    target[0] = 0.0f; target[1] = 0.0f;
  }
  glBufferSubData(GL_UNIFORM_BUFFER, sizeof(gvr::Mat4f),
    sizeof(target), (GLvoid*)target);

  // Determine which button is pressed, if any
  bool changed = false;
  int oldSelectedButton = selectedButton;
  if(controllerState.GetButtonDown(GVR_CONTROLLER_BUTTON_APP)) {
    for(int i=0; i<6; i+=2) {
      if((target[0] > (offsets[i] - 0.2f)) &&
        (target[0] < (offsets[i] + 0.2f)) &&
        (target[1] > (offsets[i+1] - 0.2f)) &&
        (target[1] < (offsets[i+1] + 0.2f))) {
          if(selectedButton != i/2) {
            selectedButton = i/2;
            changed = true;
          }
          break;
        }
    }

    // Update audio state
    if(changed) {

      // Process sound according to the selected button
      switch(selectedButton) {

        // Pause button
        case 0:
          if(audioApi->IsSoundPlaying(id)) {
            audioApi->PauseSound(id);
          }
          firstIndex = 11;
          numIndices = 6;
          break;

        // Play button
        case 1:
          if((oldSelectedButton == 0) ||
              (oldSelectedButton == 2)) {
            audioApi->ResumeSound(id);
          }
          else {
            audioApi->PlaySound(id, true);
          }
          firstIndex = 4;
          numIndices = 7;
          break;

        // Stop button
        case 2:
          audioApi->PauseSound(id);
          firstIndex = 17;
          numIndices = 7;
          break;

        default:
          break;
      }

      // Update selected button graphic
      glBufferSubData(GL_UNIFORM_BUFFER, sizeof(gvr::Mat4f) + 8*sizeof(float),
        sizeof(selectedButton), (GLvoid*)&selectedButton);
    }
  }

  // Acquire the frame and bind it
  gvr::Frame frame = swapChain->AcquireFrame();
  frame.BindBuffer(0);

  // Set uniform buffer
  glBindBuffer(GL_UNIFORM_BUFFER, ubos[0]);

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
  firstFrame = false;
}

void MediaPlayerRenderer::RenderEye(gvr::Eye eye,
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
  gvr::Mat4f lastMatrix = MatrixUtils::Transpose(mvpMatrix);

  // Pass MVP matrix to shader
  glBufferSubData(GL_UNIFORM_BUFFER, 0,
    sizeof(gvr::Mat4f), (GLvoid*)lastMatrix.m);

  // Draw player
  glUseProgram(programs[0]);  
  glBindVertexArray(vaos[0]);
  glDrawElements(GL_TRIANGLE_STRIP, 9, GL_UNSIGNED_BYTE, 0);
  glBindVertexArray(0);

  // Draw buttons
  glUseProgram(programs[1]);  
  glBindVertexArray(vaos[1]);
  glBindTexture(GL_TEXTURE_2D, tids[0]);  
  glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, 3);
  glBindVertexArray(0);

  // Draw pointer
  glUseProgram(programs[2]);  
  glBindVertexArray(vaos[2]);
  glDrawArrays(GL_TRIANGLE_FAN, 0, 16);
  glBindVertexArray(0);

  // Draw text
  glUseProgram(programs[3]);  
  glBindVertexArray(vaos[3]);
  glBindTexture(GL_TEXTURE_2D, tids[1]);
  glDrawElements(GL_TRIANGLE_STRIP, 5*numIndices, GL_UNSIGNED_BYTE, (void*)(5*firstIndex));
  glBindVertexArray(0);
}

void MediaPlayerRenderer::OnPause() {
  if(ready) {
    audioApi->Pause();
    gvrApi->PauseTracking();
  }
}

void MediaPlayerRenderer::OnResume() {
  if(ready) {
    gvrApi->ResumeTracking();
    audioApi->Resume();
  }
}

void handleMessage(GLenum source​, GLenum type​, GLuint id​,
  GLenum severity​, GLsizei length​, const GLchar* msg,
  const void* userData) {

  std::string sevMsg, typeMsg;

  // Process severity
  switch(severity​) {
    case GL_DEBUG_SEVERITY_HIGH_KHR:
      sevMsg = "High"; break;
    case GL_DEBUG_SEVERITY_MEDIUM_KHR:
      sevMsg = "Medium"; break;
    case GL_DEBUG_SEVERITY_LOW_KHR:
      sevMsg = "Low"; break;
    case GL_DEBUG_SEVERITY_NOTIFICATION_KHR:
      sevMsg = "Notification"; break;
    default:
      sevMsg = "Default";
  }

  // Process type
  switch(type​) {
    case GL_DEBUG_TYPE_ERROR_KHR:
      typeMsg = "error"; break;
    case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_KHR:
      typeMsg = "deprecated behavior"; break;
    case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_KHR:
      typeMsg = "undefined behavior"; break;
    case GL_DEBUG_TYPE_PORTABILITY_KHR:
      typeMsg = "portability"; break;
    case GL_DEBUG_TYPE_PERFORMANCE_KHR:
      typeMsg = "performance"; break;
    case GL_DEBUG_TYPE_MARKER_KHR:
      typeMsg = "marker"; break;
    case GL_DEBUG_TYPE_PUSH_GROUP_KHR:
      typeMsg = "push group"; break;
    case GL_DEBUG_TYPE_POP_GROUP_KHR:
      typeMsg = "pop group"; break;
    default:
      typeMsg = "default";
  }

  // Display the message
  __android_log_print(ANDROID_LOG_INFO, TAG,
    "%s priority %s message: %s (data: %d)",
    sevMsg.data(), typeMsg.data(), msg, (*(int*)userData));
}