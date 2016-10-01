#include "nfcd.h"
#include <cstring>

extern "C" {
    JNIEXPORT jboolean JNICALL Java_it_convergent_obliterator_xposed_Native_isEnabled(JNIEnv* env, jobject javaThis);
    JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_setEnabled(JNIEnv* env, jobject javaThis, jboolean enabled);
    JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_uploadConfiguration(JNIEnv* env, jobject javaThis, jbyte atqa, jbyte sak, jbyteArray _hist, jbyteArray _uid);
}


JNIEXPORT jboolean JNICALL Java_it_convergent_obliterator_xposed_Native_isEnabled(JNIEnv* env, jobject javaThis) {
    return patchEnabled;
}

JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_setEnabled(JNIEnv* env, jobject javaThis, jboolean enabled) {
    patchEnabled = enabled;
    if(enabled) {
        uploadPatchConfig();
    } else {
        uploadOriginalConfig();
    }
}

JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_uploadConfiguration(JNIEnv* env, jobject javaThis, jbyte atqa, jbyte sak, jbyteArray _hist, jbyteArray _uid) {
    jsize uid_len = env->GetArrayLength(_uid);
    jsize hist_len = env->GetArrayLength(_hist);
    if(uid_len > UID_MAX_LEN || hist_len > HIST_MAX_LEN) {
        jclass Exception = env->FindClass("java/lang/Exception");
        env->ThrowNew(Exception, "uid or hist bigger than buffer");
    }


    patchValues.atqa = atqa;
    patchValues.sak = sak;
    patchValues.hist_len = hist_len;
    patchValues.uid_len = uid_len;

    jbyte* uid = env->GetByteArrayElements(_uid, 0);
    jbyte* hist = env->GetByteArrayElements(_hist, 0);

    memcpy(patchValues.uid, uid, uid_len);
    memcpy(patchValues.hist, hist, hist_len);

    env->ReleaseByteArrayElements(_uid, uid, 0);
    env->ReleaseByteArrayElements(_hist, hist, 0);
}