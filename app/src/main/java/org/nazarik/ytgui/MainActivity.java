package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Инициализация UI
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // Настройка активов и git
    setupAssetsAndGit();
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Обработка результата ConsoleActivity
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1 && resultCode == RESULT_OK) {
      Log.d("ytgui", "Git setup completed successfully");
      // Переход в DownloadActivity
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
      finish();
    } else {
      Log.e("ytgui", "Git setup failed with resultCode: " + resultCode);
      // Повтор при ошибке
      setupAssetsAndGit();
    }
  }
  private void setupAssetsAndGit() {
    // Подготовка путей
    String filesDir = getFilesDir().getAbsolutePath();
    String gitPath = filesDir + "/git";
    // Копирование git
    try {
      GitUtils.copyFile(this, "git", gitPath, true);
      Log.d("ytgui", "Git binary copied successfully");
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy git binary", e);
      return;
    }
    // Настройка репозитория ytgui-env
    File envDir = new File(filesDir, "ytgui-env");
    try {
      if (!envDir.exists()) {
        // Запуск клонирования через ConsoleActivity
        Log.d("ytgui", "Starting ConsoleActivity for cloning ytgui-env via SSH");
        String command = gitPath + " clone --depth=1 git@github.com:Luwerdwighime/ytgui-env.git " + envDir.getAbsolutePath();
        Intent intent = new Intent(this, ConsoleActivity.class);
        intent.putExtra("command", command);
        startActivityForResult(intent, 1);
      } else {
        // Запуск пулла через ConsoleActivity
        Log.d("ytgui", "Starting ConsoleActivity for pulling ytgui-env via SSH");
        String command = gitPath + " -C " + envDir.getAbsolutePath() + " pull origin main";
        Intent intent = new Intent(this, ConsoleActivity.class);
        intent.putExtra("command", command);
        startActivityForResult(intent, 1);
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to setup git", e);
    }
  }
}

