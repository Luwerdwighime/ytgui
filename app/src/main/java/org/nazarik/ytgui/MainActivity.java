// MainActivity.java
package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import java.io.*;
import java.util.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

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

  // 🔄 Клонирование или обновление окружения по HTTPS
  private void syncEnvironment() {
    try {
      File envDir = new File(getFilesDir(), "ytgui-env");
      if (!envDir.exists()) {
        Git.cloneRepository()
          .setURI("https://github.com/Luwerdwighime/ytgui-env.git")
          .setDirectory(envDir)
          .call();
        log("Клонирование завершено\n");
      } else {
        Git.open(envDir).pull().call();
        log("Окружение обновлено\n");
      }

      runOnUiThread(() -> nextButton.setEnabled(true));

    } catch (GitAPIException e) {
      runOnUiThread(() ->
        Toast.makeText(this, "Ошибка сети или Git: " + e.getMessage(), Toast.LENGTH_LONG).show()
      );
      log("Ошибка клонирования: " + e.getMessage() + "\n");
    } catch (Exception ex) {
      runOnUiThread(() ->
        Toast.makeText(this, "Ошибка доступа к окружению", Toast.LENGTH_LONG).show()
      );
      log("Ошибка системы: " + ex.getMessage() + "\n");
    }
  }

  // ⚙️ Запуск yt-dlp через python внутри окружения
  private void runYtDlp(String[] options) {
    try {
      File envDir = new File(getFilesDir(), "ytgui-env");
      File pythonBin = new File(envDir, "bin/python");

      List<String> cmd = new ArrayList<>();
      cmd.add(pythonBin.getAbsolutePath());
      cmd.add("-m");
      cmd.add("yt_dlp");
      cmd.addAll(Arrays.asList(options));

      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.directory(envDir);
      pb.redirectErrorStream(true);

      java.lang.Process p = pb.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        log(line + "\n");
      }

      int code = p.waitFor();
      if (code != 0) {
        log("yt-dlp завершился с кодом " + code + "\n");
        runOnUiThread(() ->
          Toast.makeText(this, "Ошибка: yt-dlp " + code, Toast.LENGTH_LONG).show()
        );
      }

      runOnUiThread(() -> nextButton.setEnabled(true));

    } catch (Exception e) {
      log("Исключение в yt-dlp: " + e.getMessage() + "\n");
      runOnUiThread(() ->
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
      );
    }
  }

  // 🖥️ Вывод в консоль с автоскроллом
  private void log(String text) {
    runOnUiThread(() -> {
      consoleTextArea.append(text);
      consoleTextArea.setSelection(consoleTextArea.getText().length());
    });
  }
}

