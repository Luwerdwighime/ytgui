package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;

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

    File envDir = new File(getFilesDir(), "ytgui-env");

    if (!envDir.exists()) {
      log("Загружаем окружение yt-dlp...\n");
      Thread t = new Thread(this::downloadEnvironment);
      t.start();
    } else {
      Thread t = new Thread(() -> runYtDlp(options));
      t.start();
    }

    nextButton.setOnClickListener(v -> {
      startActivity(new Intent(this, DownloadActivity.class));
    });
  }

  // ⬇️ Скачивание и установка окружения
  private void downloadEnvironment() {
    try {
      URL url = new URL("https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v1.0.1.zip");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.connect();

      InputStream input = new BufferedInputStream(connection.getInputStream());
      File zipFile = new File(getFilesDir(), "env.zip");
      FileOutputStream output = new FileOutputStream(zipFile);

      byte[] buffer = new byte[4096];
      int len;
      while ((len = input.read(buffer)) != -1) {
        output.write(buffer, 0, len);
      }
      output.close();
      input.close();
      log("ZIP-файл загружен\n");

      unzip(zipFile, getFilesDir());
      zipFile.delete();

      File unpacked = new File(getFilesDir(), "ytgui-env-1.0.1");
      File envDir = new File(getFilesDir(), "ytgui-env");
      unpacked.renameTo(envDir);

      // ⛓️ Выставляем флаг выполнения на python
      File pythonBin = new File(envDir, "bin/python");
      pythonBin.setExecutable(true);
      log("Окружение установлено\n");

      runOnUiThread(() -> nextButton.setEnabled(true));

    } catch (Exception e) {
      runOnUiThread(() ->
        Toast.makeText(this, "Ошибка при скачивании окружения", Toast.LENGTH_LONG).show());
      log("Ошибка: " + e.getMessage() + "\n");
    }
  }

  // 📦 Распаковка zip
  private void unzip(File zipFile, File targetDir) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File outFile = new File(targetDir, entry.getName());
        if (entry.isDirectory()) {
          outFile.mkdirs();
        } else {
          outFile.getParentFile().mkdirs();
          try (FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
      }
    }
  }

  // ▶️ Запуск yt-dlp
  private void runYtDlp(String[] options) {
    if (options == null) return;

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
          Toast.makeText(this, "Ошибка: yt-dlp " + code, Toast.LENGTH_LONG).show());
      }

      runOnUiThread(() -> nextButton.setEnabled(true));

    } catch (Exception e) {
      log("Ошибка запуска yt-dlp: " + e.getMessage() + "\n");
      runOnUiThread(() ->
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
  }

  // 🖥️ Консоль с автоскроллом
  private void log(String text) {
    runOnUiThread(() -> {
      consoleTextArea.append(text);
      consoleTextArea.setSelection(consoleTextArea.getText().length());
    });
  }
}

