#include <jni.h>

#include "vr/gvr/capi/include/gvr.h"
#include "blankscreen_renderer.h"

extern "C" {

JNIEXPORT jlong JNICALL
  Java_com_dreambookvr_ch14_1blankscreen_BlankScreenActivity_createRenderer(
    JNIEnv *env, jclass cls, jlong context) {

  // Accept the GvrContext and return the native instance
  BlankScreenRenderer *renderer = new BlankScreenRenderer(
    reinterpret_cast<gvr_context*>(context));
  return reinterpret_cast<intptr_t>(renderer);
}

JNIEXPORT void JNICALL
  Java_com_dreambookvr_ch14_1blankscreen_BlankScreenActivity_nativeOnSurfaceCreated(
    JNIEnv *env, jclass cls, jlong renderer) {

  // Call the OnSurfaceCreated function of the native instance
  reinterpret_cast<BlankScreenRenderer *>
  (renderer)->OnSurfaceCreated();
}

JNIEXPORT void JNICALL
  Java_com_dreambookvr_ch14_1blankscreen_BlankScreenActivity_nativeOnDrawFrame(
    JNIEnv *env, jclass cls, jlong renderer) {

  // Call the OnDrawFrame function of the native instance
  reinterpret_cast<BlankScreenRenderer *>
  (renderer)->OnDrawFrame();
}


JNIEXPORT void JNICALL
  Java_com_dreambookvr_ch14_1blankscreen_BlankScreenActivity_nativeOnResume(
    JNIEnv *env, jclass cls, jlong renderer) {

  // Call the OnResume function of the native instance
  reinterpret_cast<BlankScreenRenderer *>(renderer)->OnResume();
}

JNIEXPORT void JNICALL
  Java_com_dreambookvr_ch14_1blankscreen_BlankScreenActivity_nativeOnPause(
    JNIEnv *env, jclass cls, jlong renderer) {

  // Call the OnPause function of the native instance
  reinterpret_cast<BlankScreenRenderer *>(renderer)->OnPause();
}

JNIEXPORT void JNICALL
  Java_com_dreambookvr_ch14_1blankscreen_BlankScreenActivity_nativeOnDestroy(
    JNIEnv *env, jclass cls, jlong renderer) {

  // Delete the native instance
  delete reinterpret_cast<BlankScreenRenderer *>(renderer);
}

}
