package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
  private TextView consoleTextArea;
  private Button nextButton;
  private String[] options;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    consoleTextArea = findViewById(R.id.consoleTextArea);
    nextButton = findViewById(R.id.nextButton);

    options = getIntent().getStringArrayExtra("options");
    File envDir = new File(getFilesDir(), "ytgui-env");

    if (options == null || options.length == 0) {
      log("MainActivity: запущен без параметров\n");

      if (envDir.exists()) {
        log("Окружение уже установлено — пропускаем загрузку\n");
        nextButton.setEnabled(true);
      } else {
        log("Окружение не найдено — загружаем...\n");
        prepareEnv();
      }
      return;
    }

    runYtDlp(options);
  }

  private void log(String text) {
    runOnUiThread(() -> consoleTextArea.append(text));
  }

  private void prepareEnv() {
    log("Загружаем окружение yt-dlp...\n");

    new Thread(() -> {
      try {
        String url = "https://codeload.github.com/Luwerdwighime/ytgui-env/zip/refs/tags/v1.2.2";
        log("Скачиваем: " + url + " (~148Мб)\n");

        File zipFile = new File(getCacheDir(), "env.zip");
        Utils.download(url, zipFile);

        log("Распаковываем...\n");
        Utils.extractZip(zipFile, getFilesDir());

        File extracted = new File(getFilesDir(), "ytgui-env-v1.2.2");
        File target = new File(getFilesDir(), "ytgui-env");
        if (!extracted.renameTo(target)) {
          log("Не удалось переименовать директорию\n");
        }

        log("✅ Окружение готово\n");
      } catch (Exception e) {
        log("Ошибка: " + e.getMessage() + "\n");
      }

      runOnUiThread(() -> nextButton.setEnabled(true));
    }).start();
  }

  private void runYtDlp(String[] args) {
    new Thread(() -> {
      try {
        File binDir = new File(getFilesDir(), "ytgui-env/bin");
        File envDir = new File(getFilesDir(), "ytgui-env");
        File python = new File(binDir, "python3.13");

        log("Запуск yt-dlp...\n");

        List<String> command = new ArrayList<>();
        command.add(python.getAbsolutePath());
        command.add("-m");
        command.add("yt_dlp");
        Collections.addAll(command, args);

        log("Команда:\n" + String.join(" ", command) + "\n");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PYTHONHOME", envDir.getAbsolutePath());
        pb.environment().put("FFMPEG_BINARY", new File(binDir, "ffmpeg").getAbsolutePath());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            log(line + "\n");
          }
        }

        int exitCode = process.waitFor();
        log("yt-dlp завершён с кодом " + exitCode + "\n");
      } catch (Exception e) {
        log("Ошибка запуска: " + e.getMessage() + "\n");
      }

      runOnUiThread(() -> nextButton.setEnabled(true));
    }).start();
  }
}

