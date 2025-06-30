package org.nazarik.ytgui;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConsoleActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);
    String filesDir = getFilesDir().getAbsolutePath();
    String gitBinDir = filesDir + "/git-bin";
    String sshKeyPath = gitBinDir + "/.ssh/id_rsa";
    String envDir = filesDir + "/ytgui-env";
    File sshKey = new File(sshKeyPath);
    if (!sshKey.exists()) {
      Log.e("ytgui", "SSH key not found at " + sshKeyPath);
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    String command = gitBinDir + "/git clone --depth=1 git@github.com:Luwerdwighime/ytgui-env.git " + envDir;
    try {
      // Настройка переменной окружения для SSH
      Map<String, String> env = new HashMap<>();
      env.put("GIT_SSH_COMMAND", "ssh -i " + sshKeyPath);
      ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
      pb.environment().putAll(env);
      pb.directory(new File(filesDir));
      Process process = pb.start();
      int exitCode = process.waitFor();
      if (exitCode == 0) {
        Log.d("ytgui", "Git clone succeeded");
        setResult(RESULT_OK);
      } else {
        Log.e("ytgui", "Git clone failed with exit code: " + exitCode);
        setResult(RESULT_CANCELED);
      }
    } catch (Exception e) {
      Log.e("ytgui", "Exception during git clone", e);
      setResult(RESULT_CANCELED);
    }
    finish();
  }
}

