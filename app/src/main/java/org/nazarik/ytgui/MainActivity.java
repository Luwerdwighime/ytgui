package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
      Log.d("ytgui", "Git setup completed successfully");
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
      finish();
    } else {
      Log.e("ytgui", "Git setup failed with resultCode: " + resultCode);
      setupAssetsAndGit();
    }
  }

  private void setupAssetsAndGit() {
    String filesDir = getFilesDir().getAbsolutePath();
    String gitBinDir = filesDir + "/git-bin";
    try {
      File gitBinFile = new File(gitBinDir, "git");
      if (!gitBinFile.exists()) {
        // Копирование git-bin с учётом скрытых папок
        copyFolderRecursively(this, "git-bin", gitBinDir);
        gitBinFile.setExecutable(true, false);
        File dir = new File(gitBinDir);
        dir.setReadable(true, false);
        dir.setWritable(true, false);
        setPermissionsRecursively(dir);
        Log.d("ytgui", "Git-bin folder copied and permissions set");
      } else {
        Log.d("ytgui", "Git-bin folder already exists");
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy git-bin folder", e);
      return;
    }
    File envDir = new File(filesDir, "ytgui-env");
    if (!envDir.exists()) {
      Log.d("ytgui", "Starting ConsoleActivity for cloning ytgui-env via SSH");
      String sshKeyPath = gitBinDir + "/.ssh/id_rsa";
      String command = "GIT_SSH_COMMAND='ssh -i " + sshKeyPath + "' " +
                       gitBinDir + "/git clone --depth=1 git@github.com:Luwerdwighime/ytgui-env.git " + envDir;
      Intent intent = new Intent(this, ConsoleActivity.class);
      intent.putExtra("command", command);
      startActivityForResult(intent, 1);
    }
  }

  private void copyFolderRecursively(AppCompatActivity context, String assetFolder, String destFolder) {
    try {
      File destDir = new File(destFolder);
      if (!destDir.exists()) {
        destDir.mkdirs();
      }
      String[] files = context.getAssets().list(assetFolder);
      if (files != null) {
        for (String file : files) {
          String newAssetPath = assetFolder.isEmpty() ? file : assetFolder + "/" + file;
          String[] subFiles = context.getAssets().list(newAssetPath);
          if (subFiles != null && subFiles.length > 0) {
            copyFolderRecursively(context, newAssetPath, destFolder + "/" + file);
          } else {
            java.io.InputStream in = context.getAssets().open(newAssetPath);
            java.io.File outFile = new File(destFolder, file);
            java.io.OutputStream out = new java.io.FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
              out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            outFile.setExecutable(file.endsWith("git"), false);
          }
        }
      }
    } catch (Exception e) {
      Log.e("ytgui", "Error copying folder recursively", e);
    }
  }

  private void setPermissionsRecursively(File dir) {
    dir.setReadable(true, false);
    dir.setWritable(true, false);
    dir.setExecutable(true, false);
    if (dir.isDirectory()) {
      for (File file : dir.listFiles()) {
        setPermissionsRecursively(file);
      }
    }
  }
}

