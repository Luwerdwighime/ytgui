package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.content.Intent;

import org.eclipse.jgit.api.Git;

import java.io.File;

public class MainActivity extends Activity {

  private TextView statusText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    statusText = findViewById(R.id.statusText);

    new Thread(this::syncRepository).start();
  }

  private void syncRepository() {
    try {
      File repoDir = new File(getFilesDir(), "ytgui-env");
      Git git;

      if (repoDir.exists()) {
        git = Git.open(repoDir);
        git.pull().call();
      } else {
        git = Git.cloneRepository()
          .setURI("https://github.com/Luwerdwighime/ytgui-env.git")
          .setDirectory(repoDir)
          .setCloneAllBranches(false)
          .setDepth(1)
          .call();
      }

      git.close();
    } catch (Exception e) {
      runOnUiThread(() -> statusText.setText("Ошибка: " + e.getMessage()));
      return;
    }

    new Handler(getMainLooper()).post(() -> {
      startActivity(new Intent(this, DownloadActivity.class));
      finish();
    });
  }
}

