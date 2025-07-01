package org.nazarik.ytgui;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class ConsoleActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);
    TextView consoleOutput = findViewById(R.id.consoleOutput);
    String filesDir = getFilesDir().getAbsolutePath();
    String gitBinDir = filesDir + "/git-bin";
    String sshKeyPath = gitBinDir + "/ssh/id_rsa";
    File sshKey = new File(sshKeyPath);
    if (!sshKey.exists()) {
      Log.e("ytgui", "SSH key not found at " + sshKeyPath);
      if (consoleOutput != null) {
        consoleOutput.append("ОШИБКА: SSH ключ не найден.\n");
      }
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    String command = getIntent().getStringExtra("command");
    if (command == null) {
      Log.e("ytgui", "No command provided");
      if (consoleOutput != null) {
        consoleOutput.append("ОШИБКА: Команда не предоставлена.\n");
      }
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    // Подготовка данных для Subproc
    String[] commandParts = command.split("\\s+", 2);
    String executable = gitBinDir + "/git";
    String[] options = (commandParts.length > 1 && !commandParts[1].isEmpty()) ? commandParts[1].split("\\s+") : new String[0];
    String[] envVars = new String[]{"GIT_SSH_COMMAND=ssh -i " + sshKeyPath};
    Log.d("ytgui", "Executing: " + executable + " with options: " + String.join(" ", options));
    // Создание и запуск процесса
    Subproc subproc = new Subproc(this, executable, options, envVars, consoleOutput);
    subproc.run(exitCode -> {
      Log.d("ytgui", "Process completed with exit code: " + exitCode);
      if (exitCode == 0) {
        setResult(RESULT_OK);
      } else {
        if (consoleOutput != null) {
          consoleOutput.append("ОШИБКА: Команда завершилась с кодом " + exitCode + ".\n");
        }
        setResult(RESULT_CANCELED);
      }
      finish();
    });
  }
}

