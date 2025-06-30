package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import java.io.File;
public class MainActivity extends AppCompatActivity {
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
    try {
      GitUtils.copyFile(this, "git", gitPath, true);
      GitUtils.copyFile(this, "ca-certificates.crt", certPath, false);
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
        String command = gitPath + " clone --depth=1 https://github.com/Luwerdwighime/ytgui-env.git " + envDir.getAbsolutePath();
        GitUtils.runCommand(this, command, null, new String[] {"GIT_SSL_CAINFO=" + certPath});
      } else {
        // Пулл ytgui-env
        Log.d("ytgui", "Pulling ytgui-env");
        String command = gitPath + " -C " + envDir.getAbsolutePath() + " pull origin main";
        GitUtils.runCommand(this, command, null, new String[] {"GIT_SSL_CAINFO=" + certPath});
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to setup git", e);
    }
  }
}

