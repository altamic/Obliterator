#include "nfcd.h"
#include <cstring>

extern "C" {
    JNIEXPORT jboolean JNICALL Java_it_convergent_obliterator_xposed_Native_isEnabled(JNIEnv* env, jobject javaThis);
    JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_setEnabled(JNIEnv* env, jobject javaThis, jboolean enabled);
    JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_uploadConfiguration(JNIEnv* env, jobject javaThis, jbyte atqa, jbyte sak, jbyteArray _uid, jbyteArray _data);
}


JNIEXPORT jboolean JNICALL Java_it_convergent_obliterator_xposed_Native_isEnabled(JNIEnv* env, jobject javaThis) {
    return patchEnabled;
}

JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_setEnabled(JNIEnv* env, jobject javaThis, jboolean enabled) {
    patchEnabled = enabled;
    if (enabled) {
        uploadPatchConfig();
    } else {
        uploadOriginalConfig();
    }
}

JNIEXPORT void JNICALL Java_it_convergent_obliterator_xposed_Native_uploadConfiguration(JNIEnv* env, jobject javaThis, jbyte atqa, jbyte sak, jbyteArray _uid, jbyteArray _data) {
    jsize uid_len = env->GetArrayLength(_uid);
//    jsize data_len = env->GetArrayLength(_data);
    if (uid_len > UID_MAX_LEN) {
        jclass Exception = env->FindClass("java/lang/Exception");
        env->ThrowNew(Exception, "uid bigger than buffer");
    }

    patchValues.atqa = atqa;
    patchValues.sak = sak;
    patchValues.uid_len = uid_len;

    jbyte* uid = env->GetByteArrayElements(_uid, 0);
//    jbyte* data = env->GetByteArrayElements(_data, 0);

    memcpy(patchValues.uid, uid, uid_len);
//    memcpy(patchValues.data, data, data_len);

    env->ReleaseByteArrayElements(_uid, uid, 0);
//    env->ReleaseByteArrayElements(_data, data, 0);
}