#include <jni.h>
#include "vosk_api.h"

extern "C" {
JNIEXPORT jlong JNICALL Java_com_alphacephei_vosk_Recognizer_init(JNIEnv *env, jobject obj, jlong model_ptr, jfloat sample_rate) {
    Model *model = (Model *) model_ptr;
    return (jlong) new Recognizer(*model, sample_rate);
}
JNIEXPORT void JNICALL Java_com_alphacephei_vosk_Recognizer_free(JNIEnv *env, jobject obj, jlong ptr) {
Recognizer *recognizer = (Recognizer *) ptr;
delete recognizer;
}
JNIEXPORT jboolean JNICALL Java_com_alphacephei_vosk_Recognizer_acceptWaveform(JNIEnv *env, jobject obj, jlong ptr, jbyteArray data) {
Recognizer *recognizer = (Recognizer *) ptr;
jbyte *bytes = env->GetByteArrayElements(data, NULL);
jsize length = env->GetArrayLength(data);
bool result = recognizer->AcceptWaveform((const char *) bytes, length);
env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
return result;
}
JNIEXPORT jstring JNICALL Java_com_alphacephei_vosk_Recognizer_getResult(JNIEnv *env, jobject obj, jlong ptr) {
Recognizer *recognizer = (Recognizer *) ptr;
return env->NewStringUTF(recognizer->Result().c_str());
}
JNIEXPORT jstring JNICALL Java_com_alphacephei_vosk_Recognizer_getFinalResult(JNIEnv *env, jobject obj, jlong ptr) {
Recognizer *recognizer = (Recognizer *) ptr;
return env->NewStringUTF(recognizer->FinalResult().c_str());
}
JNIEXPORT jstring JNICALL Java_com_alphacephei_vosk_Recognizer_getPartialResult(JNIEnv *env, jobject obj, jlong ptr) {
Recognizer *recognizer = (Recognizer *) ptr;
return env->NewStringUTF(recognizer->PartialResult().c_str());
}
}
