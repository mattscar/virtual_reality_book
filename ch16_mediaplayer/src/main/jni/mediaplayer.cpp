#include <jni.h>

#include <android/asset_manager_jni.h>

#include <utility>

#include "vr/gvr/capi/include/gvr.h"
#include "mediaplayer_renderer.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_dreambookvr_ch16_1mediaplayer_MediaPlayerActivity_createRenderer(
    JNIEnv *env, jclass cls, jlong gvrContext, jobject assetMgr, 
    jobject loader, jobject appContext) {

  // Create a new AudioApi
  std::unique_ptr<gvr::AudioApi> audioInst(new gvr::AudioApi);

  // Initialize the AudioApi
  audioInst->Init(env, appContext, loader,
    GVR_AUDIO_RENDERING_BINAURAL_LOW_QUALITY);    

  MediaPlayerRenderer *renderer =
    new MediaPlayerRenderer(reinterpret_cast<gvr_context*>(gvrContext),
      AAssetManager_fromJava(env, assetMgr), std::move(audioInst));
  return reinterpret_cast<intptr_t>(renderer);
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch16_1mediaplayer_MediaPlayerActivity_nativeOnSurfaceCreated(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<MediaPlayerRenderer *>(renderer)->OnSurfaceCreated();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch16_1mediaplayer_MediaPlayerActivity_nativeOnDrawFrame(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<MediaPlayerRenderer *>(renderer)->OnDrawFrame();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch16_1mediaplayer_MediaPlayerActivity_nativeOnResume(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<MediaPlayerRenderer *>(renderer)->OnResume();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch16_1mediaplayer_MediaPlayerActivity_nativeOnPause(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<MediaPlayerRenderer *>(renderer)->OnPause();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch16_1mediaplayer_MediaPlayerActivity_nativeOnDestroy(
    JNIEnv *env, jclass cls, jlong renderer) {

  delete reinterpret_cast<MediaPlayerRenderer *>(renderer);
}

}
