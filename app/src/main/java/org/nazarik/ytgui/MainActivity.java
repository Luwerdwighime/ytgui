// MainActivity.java
package org.nazarik.ytgui;

import android.app.Activity;
import android.os.*;
import android.widget.*;
import java.io.*;
import java.util.Arrays;
import org.eclipse.jgit.api.Git;

public class MainActivity extends Activity {
  private Button nextButton;
  private EditText consoleTextArea;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    nextButton = findViewById(R.id.nextButton);
    consoleTextArea = findViewById(R.id.consoleTextArea);

    String[] options = getIntent().getStringArrayExtra("options");

    if (options == null) {
      log("Качаем yt-dlp... ~500Мб\n");
      Thread t = new Thread(this::syncEnvironment);
      t.start();
    } else {
      Thread t = new Thread(() -> runYtDlp(options));
      t.start();
    }

    nextButton.setOnClickListener(v -> {
      startActivity(new Intent(this, DownloadActivity.class));
    });
  }

  private void syncEnvironment() {
    try {
      File envDir = new File(getFilesDir(), "ytgui-env");
      if (!envDir.exists()) {
        Git.cloneRepository()
          .setURI("https://github.com/Luwerdwighime/ytgui-env")
          .setDirectory(envDir)
          .setDepth(1)
          .call();
        log("Клонирование завершено\n");
      } else {
        Git.open(envDir).pull().call();
        log("Окружение обновлено\n");
      }
      runOnUiThread(() -> nextButton.setEnabled(true));
    } catch (Exception e) {
      runOnUiThread(() -> Toast.makeText(this, "Сеть отсутствует", Toast.LENGTH_LONG).show());
      log("Ошибка при загрузке окружения\n");
    }
  }

  private void runYtDlp(String[] options) {
    try {
      File envDir = new File(getFilesDir(), "ytgui-env");
      File binDir = new File(envDir, "bin");

      Process p = new ProcessBuilder("python3", "-m", "yt_dlp")
        .directory(binDir)
        .command("python3", "-m", "yt_dlp", options)
        .redirectErrorStream(true)
        .start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        log(line + "\n");
      }

      int code = p.waitFor();
      if (code != 0) {
        log("yt-dlp завершился с кодом " + code + "\n");
        runOnUiThread(() -> Toast.makeText(this, "Ошибка: yt-dlp " + code, Toast.LENGTH_LONG).show());
      }

      runOnUiThread(() -> nextButton.setEnabled(true));

    } catch (Exception e) {
      log("Исключение в yt-dlp: " + e.getMessage() + "\n");
      runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
  }

  private void log(String text) {
    runOnUiThread(() -> {
      consoleTextArea.append(text);
      consoleTextArea.setSelection(consoleTextArea.getText().length());
    });
  }
}

