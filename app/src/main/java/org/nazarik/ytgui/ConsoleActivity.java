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
    Log.d("ytgui", "Received command: " + command);
    // Подготовка данных для Subproc
    String executable = gitBinDir + "/git";
    String[] envVars = new String[]{"GIT_SSH_COMMAND=ssh -i " + sshKeyPath};
    // Извлекаем только аргументы команды (clone и далее)
    String[] commandParts = command.split("\\s+", 2);
    String[] options = (commandParts.length > 1) ? commandParts[1].replaceFirst("GIT_SSH_COMMAND='ssh -i " + sshKeyPath + "'\\s*", "").split("\\s+") : new String[0];
    Log.d("ytgui", "Executable: " + executable + ", Options: " + String.join(" ", options));
    // Создание и запуск процесса
    Subproc subproc = new Subproc(this, executable, options, envVars, consoleOutput);
    subproc.run(exitCode -> {
      Log.d("ytgui", "Subproc callback with exit code: " + exitCode);
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

