#include <jni.h>

#include <android/asset_manager_jni.h>

#include "vr/gvr/capi/include/gvr.h"
#include "spinningtexture_renderer.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_dreambookvr_ch15_1spinningtexture_SpinningTextureActivity_createRenderer(
    JNIEnv *env, jclass cls, jobject asset_mgr, jlong context) {

  SpinningTextureRenderer *renderer =
      new SpinningTextureRenderer(AAssetManager_fromJava(env, asset_mgr), reinterpret_cast<gvr_context*>(context));
  return reinterpret_cast<intptr_t>(renderer);
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch15_1spinningtexture_SpinningTextureActivity_nativeOnSurfaceCreated(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<SpinningTextureRenderer *>(renderer)->OnSurfaceCreated();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch15_1spinningtexture_SpinningTextureActivity_nativeOnDrawFrame(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<SpinningTextureRenderer *>(renderer)->OnDrawFrame();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch15_1spinningtexture_SpinningTextureActivity_nativeOnResume(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<SpinningTextureRenderer *>(renderer)->OnResume();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch15_1spinningtexture_SpinningTextureActivity_nativeOnPause(
    JNIEnv *env, jclass cls, jlong renderer) {

  reinterpret_cast<SpinningTextureRenderer *>(renderer)->OnPause();
}

JNIEXPORT void JNICALL
Java_com_dreambookvr_ch15_1spinningtexture_SpinningTextureActivity_nativeOnDestroy(
    JNIEnv *env, jclass cls, jlong renderer) {

  delete reinterpret_cast<SpinningTextureRenderer *>(renderer);
}

}
