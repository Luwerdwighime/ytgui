package org.nazarik.ytgui;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;

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
    Bundle extras = getIntent().getExtras();
    String command = extras != null ? extras.getString("command") : null;
    if (command == null) {
      Log.e("ytgui", "No command provided in extras");
      if (consoleOutput != null) {
        consoleOutput.append("ОШИБКА: Команда не предоставлена.\n");
      }
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    Log.d("ytgui", "Received raw command: " + command);
    // Ручное разбиение команды
    String executable = gitBinDir + "/git";
    String[] envVars = new String[]{"GIT_SSH_COMMAND=ssh -i " + sshKeyPath};
    ArrayList<String> optionsList = new ArrayList<>();
    String[] parts = command.split("\\s+");
    boolean inEnv = false;
    for (String part : parts) {
      if (part.startsWith("GIT_SSH_COMMAND=")) {
        inEnv = true;
        continue;
      }
      if (inEnv && part.contains("'")) {
        inEnv = false;
        continue;
      }
      if (!inEnv && !part.isEmpty() && !part.equals(executable)) {
        optionsList.add(part);
      }
    }
    String[] options = optionsList.toArray(new String[0]);
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

