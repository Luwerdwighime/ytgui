package org.nazarik.ytgui;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import java.io.File;
public class MainActivity extends AppCompatActivity {
  static {
    // Загрузка нативной библиотеки
    try {
      System.loadLibrary("ytgui");
      Log.d("ytgui", "Loaded native library");
    } catch (UnsatisfiedLinkError e) {
      Log.e("ytgui", "Failed to load native library", e);
    }
  }
  public native void copyFile(AssetManager assetManager, String assetPath, String outputPath);
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Инициализация UI
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // Копирование активов и настройка git
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
    try {
      AssetManager assets = getAssets();
      if (assets == null) {
        Log.e("ytgui", "AssetManager is null");
        return;
      }
      copyFile(assets, "git", gitPath);
      copyFile(assets, "ca-certificates.crt", certPath);
      Log.d("ytgui", "Assets copied successfully");
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy assets", e);
      return;
    }
    // Настройка репозитория ytgui-env
    File envDir = new File(filesDir, "ytgui-env");
    try {
      if (!envDir.exists()) {
        // Клонирование ytgui-env
        Log.d("ytgui", "Cloning ytgui-env");
        Process process = Runtime.getRuntime().exec(new String[] {
          gitPath, "clone", "--depth=1", "https://github.com/Luwerdwighime/ytgui-env.git", envDir.getAbsolutePath()
        }, new String[] {"GIT_SSL_CAINFO=" + certPath});
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
        process.waitFor();
        if (process.exitValue() != 0) {
          Log.e("ytgui", "Failed to pull ytgui-env");
        }
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to setup git", e);
    }
  }
}

