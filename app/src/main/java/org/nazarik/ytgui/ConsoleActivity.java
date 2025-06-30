package org.nazarik.ytgui;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConsoleActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);
    String filesDir = getFilesDir().getAbsolutePath();
    String gitBinDir = filesDir + "/git-bin";
    String sshKeyPath = gitBinDir + "/ssh/id_rsa";
    String envDir = filesDir + "/ytgui-env";
    File sshKey = new File(sshKeyPath);
    if (!sshKey.exists()) {
      Log.e("ytgui", "SSH key not found at " + sshKeyPath);
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    String command = getIntent().getStringExtra("command");
    if (command == null) {
      Log.e("ytgui", "No command provided");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    try {
      // Разбиваем команду и добавляем через ProcessBuilder
      String[] commandParts = command.split("\\s+");
      List<String> commandList = new ArrayList<>();
      for (String part : commandParts) {
        if (!part.isEmpty()) {
          commandList.add(part);
        }
      }
      ProcessBuilder pb = new ProcessBuilder(commandList);
      // Устанавливаем переменную окружения
      pb.environment().put("GIT_SSH_COMMAND", "ssh -i " + sshKeyPath);
      pb.directory(new File(filesDir));
      Process process = pb.start();
      int exitCode = process.waitFor();
      if (exitCode == 0) {
        setResult(RESULT_OK);
      } else {
        setResult(RESULT_CANCELED);
      }
    } catch (Exception e) {
      Log.e("ytgui", "Exception during git clone", e);
      setResult(RESULT_CANCELED);
    }
    finish();
  }
}

