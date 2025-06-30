#include <jni.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <string>
#include <fstream>
#include <sys/stat.h>
void copyFile(AAssetManager* mgr, const std::string& assetPath, const std::string& outputPath) {
  // Проверка AssetManager
  if (!mgr) {
    __android_log_print(ANDROID_LOG_ERROR, "ytgui", "AssetManager is null");
    return;
  }
  // Открытие актива
  AAsset* asset = AAssetManager_open(mgr, assetPath.c_str(), AASSET_MODE_STREAMING);
  if (!asset) {
    __android_log_print(ANDROID_LOG_ERROR, "ytgui", "Failed to open asset: %s", assetPath.c_str());
    return;
  }
  // Открытие выходного файла
  std::ofstream out(outputPath, std::ios::binary);
  if (!out) {
    AAsset_close(asset);
    __android_log_print(ANDROID_LOG_ERROR, "ytgui", "Failed to open output file: %s", outputPath.c_str());
    return;
  }
  // Копирование данных
  char buffer[1024];
  ssize_t bytes;
  while ((bytes = AAsset_read(asset, buffer, sizeof(buffer))) > 0) {
    out.write(buffer, bytes);
  }
  if (bytes < 0) {
    __android_log_print(ANDROID_LOG_ERROR, "ytgui", "Failed to read asset: %s", assetPath.c_str());
    AAsset_close(asset);
    out.close();
    return;
  }
  // Закрытие файлов
  AAsset_close(asset);
  out.close();
  // Установка прав для git
  if (assetPath == "git") {
    if (chmod(outputPath.c_str(), 0700) != 0) {
      __android_log_print(ANDROID_LOG_ERROR, "ytgui", "Failed to set executable permissions for %s", outputPath.c_str());
      return;
    }
  }
  // Логирование успеха
  __android_log_print(ANDROID_LOG_INFO, "ytgui", "Copied %s to %s", assetPath.c_str(), outputPath.c_str());
}
extern "C" JNIEXPORT void JNICALL
Java_org_nazarik_ytgui_MainActivity_copyFile(JNIEnv* env, jobject /* this */, jobject assetManager, jstring assetPath, jstring outputPath) {
  // Получение AssetManager
  AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
  if (!mgr) {
    __android_log_print(ANDROID_LOG_ERROR, "ytgui", "Failed to get AssetManager");
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Failed to get AssetManager");
    return;
  }
  // Получение строк
  const char* assetPathStr = env->GetStringUTFChars(assetPath, nullptr);
  if (!assetPathStr) {
    __android_log_print(ANDROID_LOG_ERROR, "ytgui", "Failed to get assetPath string");
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Failed to get assetPath string");
    return;
  }
  const char* outputPathStr = env->GetStringUTFChars(outputPath, nullptr);
  if (!outputPathStr) {
    env->ReleaseStringUTFChars(assetPath, assetPathStr);
    __android_log_print(ANDROID_LOG_ERROR, "ytgui", "Failed to get outputPath string");
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Failed to get outputPath string");
    return;
  }
  // Вызов копирования
  copyFile(mgr, assetPathStr, outputPathStr);
  // Освобождение строк
  env->ReleaseStringUTFChars(assetPath, assetPathStr);
  env->ReleaseStringUTFChars(outputPath, outputPathStr);
}

