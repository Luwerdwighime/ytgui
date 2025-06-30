package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupAssetsAndGit();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1 && resultCode == RESULT_OK) {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
      finish();
    } else {
      TextView consoleOutput = findViewById(R.id.consoleOutput);
      if (consoleOutput != null) {
        consoleOutput.append("ОШИБКА: Нет связи.\nПерезапустите приложение\n");
      }
      finish();
    }
  }

  private void setupAssetsAndGit() {
    String filesDir = getFilesDir().getAbsolutePath();
    String gitBinDir = filesDir + "/git-bin";
    try {
      File gitBinFile = new File(gitBinDir, "git");
      if (!gitBinFile.exists()) {
        GitUtils.copyFolder(this, "git-bin", gitBinDir);
        gitBinFile.setExecutable(true, false);
        File sshDir = new File(gitBinDir + "/ssh");
        if (!sshDir.exists()) {
          Log.w("ytgui", "ssh folder not found after copy");
        }
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy git-bin folder", e);
      return;
    }
    File envDir = new File(filesDir, "ytgui-env");
    if (!envDir.exists()) {
      String sshKeyPath = gitBinDir + "/ssh/id_rsa";
      String command = "GIT_SSH_COMMAND='ssh -i " + sshKeyPath + "' " +
                       gitBinDir + "/git clone --depth=1 git@github.com:Luwerdwighime/ytgui-env.git " + envDir;
      Intent intent = new Intent(this, ConsoleActivity.class);
      intent.putExtra("command", command);
      startActivityForResult(intent, 1);
    }
  }
}

