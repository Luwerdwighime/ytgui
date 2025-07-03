package org.nazarik.ytgui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import java.io.File;

public class MainActivity extends AppCompatActivity {
  private static final String REPO_URL = "https://github.com/Luwerdwighime/ytgui-env.git";
  private static final String REPO_DIR = "/ytgui-env";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Запускаем загрузку репы в отдельном потоке
    new Thread(() -> {
      try {
        File repoDir = new File(getFilesDir() + REPO_DIR);
        if (repoDir.exists()) {
          // Если папка существует, делаем pull
          Git.open(repoDir).pull().call();
        } else {
          // Иначе клонируем с глубиной 1
          Git.cloneRepository()
            .setURI(REPO_URL)
            .setDirectory(repoDir)
            .setDepth(1)
            .call();
        }
        // Переход в DownloadActivity по завершении
        runOnUiThread(() -> {
          startActivity(new Intent(this, DownloadActivity.class));
          finish();
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }
}

