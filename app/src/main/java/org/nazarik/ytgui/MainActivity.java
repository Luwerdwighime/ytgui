package org.nazarik.ytgui;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import java.io.File;
public class MainActivity extends AppCompatActivity {
  // Загрузка нативной библиотеки
  static {
    try {
      System.loadLibrary("ytgui");
      Log.d("ytgui", "Loaded native library");
    } catch (UnsatisfiedLinkError e) {
      Log.e("ytgui", "Failed to load native library", e);
    }
  }
  // Объявление нативной функции
  public native void copyFile(AssetManager assetManager, String assetPath, String outputPath);
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Инициализация UI
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // Настройка активов и git
    setupAssetsAndGit();
    // Настройка кнопки перехода
    Button nextButton = findViewById(R.id.nextButton);
    nextButton.setOnClickListener(v -> {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
    });
  }
  private void setupAssetsAndGit() {
    // Подготовка путей
    String filesDir = getFilesDir().getAbsolutePath();
    String gitPath = filesDir + "/git";
    String certPath = filesDir + "/ca-certificates.crt";
    // Копирование активов
    AssetManager assets = getAssets();
    if (assets == null) {
      Log.e("ytgui", "AssetManager is null");
      return;
    }
    if (!copyAsset(assets, "git", gitPath) || !copyAsset(assets, "ca-certificates.crt", certPath)) {
      Log.e("ytgui", "Failed to copy assets");
      return;
    }
    Log.d("ytgui", "Assets copied successfully");
    // Настройка репозитория ytgui-env
    File envDir = new File(filesDir, "ytgui-env");
    try {
      if (!envDir.exists()) {
        // Клонирование ytgui-env
        Log.d("ytgui", "Cloning ytgui-env");
        Process process = Runtime.getRuntime().exec(new String[] {
          gitPath, "clone", "--depth=1", "https://github.com/Luwerdwighime/ytgui-env.git", envDir.getAbsolutePath()
        }, new String[] {"GIT_SSL_CAINFO=" + certPath});
        logProcessOutput(process);
        process.waitFor();
        if (process.exitValue() != 0) {
          Log.e("ytgui", "Failed to clone ytgui-env");
        }
      } else {
        // Пулл ytgui-env
        Log.d("ytgui", "Pulling ytgui-env");
        Process process = Runtime.getRuntime().exec(new String[] {
          gitPath, "-C", envDir.getAbsolutePath(), "pull", "origin", "main"
        }, new String[] {"GIT_SSL_CAINFO=" + certPath});
        logProcessOutput(process);
        process.waitFor();
        if (process.exitValue() != 0) {
          Log.e("ytgui", "Failed to pull ytgui-env");
        }
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to setup git", e);
    }
  }
  private boolean copyAsset(AssetManager assets, String assetPath, String outputPath) {
    // Копирование одного актива
    try {
      copyFile(assets, assetPath, outputPath);
      Log.d("ytgui", "Copied " + assetPath + " to " + outputPath);
      return true;
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy " + assetPath, e);
      return false;
    }
  }
  private void logProcessOutput(Process process) {
    // Логирование вывода процесса
    try {
      java.io.BufferedReader stdout = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
      java.io.BufferedReader stderr = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
      String line;
      while ((line = stdout.readLine()) != null) {
        Log.d("ytgui", "Git stdout: " + line);
      }
      while ((line = stderr.readLine()) != null) {
        Log.e("ytgui", "Git stderr: " + line);
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to log process output", e);
    }
  }
}

