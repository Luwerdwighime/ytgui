#include <jni.h>
#include <android/asset_manager_jni.h>
#include <string>
#include <fstream>
#include <android/log.h>
void copyFile(
  JNIEnv* env, AAssetManager* mgr,
  const std::string& assetPath,
  const std::string& outputPath
) {
  AAsset* asset = AAssetManager_open(
    mgr, assetPath.c_str(),
    AASSET_MODE_STREAMING
  );
  if (!asset) {
    __android_log_print(
      ANDROID_LOG_ERROR, "Ytgui",
      "Failed to open asset: %s",
      assetPath.c_str()
    );
    return;
  }
  std::ofstream out(outputPath, std::ios::binary);
  if (!out) {
    AAsset_close(asset);
    __android_log_print(
      ANDROID_LOG_ERROR, "Ytgui",
      "Failed to open output file: %s",
      outputPath.c_str()
    );
    return;
  }
  char buffer[1024];
  ssize_t bytes;
  while ((bytes = AAsset_read(
    asset, buffer, sizeof(buffer))
  ) > 0) out.write(buffer, bytes);
  AAsset_close(asset);
  out.close();
  if (assetPath == "git") {
    chmod(outputPath.c_str(), 0700);
  }
  __android_log_print(
    ANDROID_LOG_INFO, "Ytgui",
    "Copied %s to %s",
    assetPath.c_str(), outputPath.c_str()
  );
}

extern "C" JNIEXPORT void JNICALL
Java_org_nazarik_ytgui_MainActivity_copyFile(JNIEnv* env, jobject /* this */, jstring assetPath, jstring outputPath) {
  jclass clazz = env->FindClass("android/content/res/AssetManager");
  jmethodID getAssets = env->GetMethodID(clazz, "getAssets", "()Landroid/content/res/AssetManager;");
  jobject assetManager = env->CallObjectMethod(env->GetObjectClass(env->GetCurrentApplication()), getAssets);
  AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
  const char* assetPathStr = env->GetStringUTFChars(assetPath, nullptr);
  const char* outputPathStr = env->GetStringUTFChars(outputPath, nullptr);
  copyFile(env, mgr, assetPathStr, outputPathStr);
  env->ReleaseStringUTFChars(assetPath, assetPathStr);
  env->ReleaseStringUTFChars(outputPath, outputPathStr);
}

