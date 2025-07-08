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
  private static final String ENV_VERSION = "1.2.2";
  private static final String ENV_ZIP_NAME = "ytgui-env-" + ENV_VERSION + ".zip";
  private static final String ENV_FOLDER_NAME = "ytgui-env-" + ENV_VERSION;
  private static final String ENV_FINAL_NAME = "ytgui-env";
  private static final String ENV_URL = "https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v" + ENV_VERSION + ".zip";

  private Button nextButton;
  private EditText consoleTextArea;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    nextButton = findViewById(R.id.nextButton);
    consoleTextArea = findViewById(R.id.consoleTextArea);

    String[] options = getIntent().getStringArrayExtra("options");

    File envDir = new File(getFilesDir(), ENV_FINAL_NAME);

    if (!envDir.exists()) {
      log("Загружаем окружение yt-dlp " + ENV_VERSION + " (~146Мб)...\n");
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

  private void downloadEnvironment() {
    try {
      URL url = new URL(ENV_URL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.connect();

      InputStream input = new BufferedInputStream(connection.getInputStream());
      File zipFile = new File(getFilesDir(), ENV_ZIP_NAME);
      FileOutputStream output = new FileOutputStream(zipFile);

      byte[] buffer = new byte[4096];
      int len;
      while ((len = input.read(buffer)) != -1) {
        output.write(buffer, 0, len);
      }
      output.close();
      input.close();
      log("ZIP-файл загружен. Распаковываем...\n");

      unzip(zipFile, getFilesDir());
      zipFile.delete();

      File unpacked = new File(getFilesDir(), ENV_FOLDER_NAME);
      File envDir = new File(getFilesDir(), ENV_FINAL_NAME);
      unpacked.renameTo(envDir);
      File python313 = new File(envDir, "bin/python3.13");
      python313.setExecutable(true);
      log("Окружение установлено\n");

      runOnUiThread(() -> nextButton.setEnabled(true));
    } catch (Exception e) {
      runOnUiThread(() ->
        Toast.makeText(this, "Ошибка при скачивании окружения", Toast.LENGTH_LONG).show());
      log("Ошибка: " + e.getMessage() + "\n");
    }
  }

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

  private void runYtDlp(String[] options) {
    if (options == null) return;

    try {
      File envDir = new File(getFilesDir(), ENV_FINAL_NAME);
      File pythonBin = new File(envDir, "bin/python3.13");

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

  private void log(String text) {
    runOnUiThread(() -> {
      consoleTextArea.append(text);
      consoleTextArea.setSelection(consoleTextArea.getText().length());
    });
  }
}

