// файл: app/src/main/java/org/nazarik/ytgui/MainActivity.java
package org.nazarik.ytgui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
  private TextView consoleView;
  private ScrollView consoleScroll;
  private Button btnNext;

  private final String envVersion = "v1.2.3";
  private final File envRoot = new File(
    "/data/data/org.nazarik.ytgui/files/ytgui-env");
  private final File pythonBin = new File(envRoot, "bin/python3.13");
  private final File ffmpegBin = new File(envRoot, "bin/ffmpeg");
  private final String zipUrl = "https://github.com/"
    + "Luwerdwighime/ytgui-env/archive/refs/tags/" + envVersion + ".zip";

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    setContentView(R.layout.activity_main);

    consoleView = findViewById(R.id.consoleView);
    consoleScroll = findViewById(R.id.consoleScroll);
    btnNext = findViewById(R.id.btnNext);

    btnNext.setOnClickListener(v -> {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
    });

    List<String> options = getIntent().getStringArrayListExtra("options");

    if (options == null) {
      checkOrInstallEnv();
    } else {
      runYtDlp(options);
    }
  }

  private void checkOrInstallEnv() {
    if (envRoot.exists()) {
      if (pythonBin.exists()) {
        writeConsole(getString(R.string.env_ready));
        btnNext.setEnabled(true);
      } else {
        writeConsole(getString(R.string.env_error));
      }
      return;
    }

    writeConsole(String.format(
      getString(R.string.env_download), envVersion));
    writeConsole(zipUrl);

    new Thread(() -> {
      try {
        File zipFile = new File(getFilesDir(), "env.zip");
        downloadZip(zipUrl, zipFile);

        writeConsole("Распаковка ytgui-env");
        unzip(zipFile, getFilesDir());

        File unpacked = new File(getFilesDir(),
          "ytgui-env-" + envVersion);
        unpacked.renameTo(envRoot);

        pythonBin.setExecutable(true);
        ffmpegBin.setExecutable(true);

        writeConsole(getString(R.string.env_ready));
        runOnUiThread(() -> btnNext.setEnabled(true));
      } catch (Exception e) {
        writeConsole("Ошибка установки: " + e.getMessage());
      }
    }).start();
  }

  private void runYtDlp(List<String> options) {
    if (!pythonBin.exists()) {
      writeConsole(getString(R.string.env_error));
      return;
    }

    new Thread(() -> {
      try {
        File docs = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_DOCUMENTS);
        File outDir = docs;

        for (String opt : options) {
          if (opt.contains("playlist") && opt.contains("video"))
            outDir = new File(docs, "ytVideo");
          else if (opt.contains("playlist") && opt.contains("audio"))
            outDir = new File(docs, "ytAudio");
          else if (opt.contains("video"))
            outDir = new File(docs, "ytVideo");
          else
            outDir = new File(docs, "ytAudio");
        }
        outDir.mkdirs();

        options.add("--output");
        options.add(outDir + "/%(title)s.%(ext)s");

        String[] cmd = new String[options.size() + 3];
        cmd[0] = pythonBin.getAbsolutePath();
        cmd[1] = "-m";
        cmd[2] = "yt_dlp";
        for (int i = 0; i < options.size(); i++)
          cmd[3 + i] = options.get(i);

        Process proc = new ProcessBuilder(cmd)
          .redirectErrorStream(true).start();

        BufferedReader reader = new BufferedReader(
          new InputStreamReader(proc.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null)
          writeConsole(line);

        int code = proc.waitFor();
        runOnUiThread(() -> btnNext.setEnabled(true));
        if (code != 0) {
          writeConsole("yt-dlp завершился с кодом " + code);
          Toast.makeText(this,
            "Ошибка: yt-dlp exit " + code,
            Toast.LENGTH_LONG).show();
        }
      } catch (Exception e) {
        writeConsole("Ошибка загрузки: " + e.getMessage());
      }
    }).start();
  }

  private void writeConsole(String msg) {
    runOnUiThread(() -> {
      consoleView.append(msg + "\n");
      consoleScroll.post(() ->
        consoleScroll.fullScroll(ScrollView.FOCUS_DOWN));
    });
  }

  private void downloadZip(String urlStr, File target) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
      throw new IOException("HTTP " + conn.getResponseCode());

    InputStream in = conn.getInputStream();
    OutputStream out = new FileOutputStream(target);
    byte[] buf = new byte[4096];
    int n;
    while ((n = in.read(buf)) != -1)
      out.write(buf, 0, n);
    out.close(); in.close(); conn.disconnect();
  }

  private void unzip(File zipFile, File targetDir) throws IOException {
    ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
    ZipEntry entry;
    while ((entry = zis.getNextEntry()) != null) {
      File outFile = new File(targetDir, entry.getName());
      if (entry.isDirectory()) {
        outFile.mkdirs();
      } else {
        outFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(outFile);
        byte[] buf = new byte[4096];
        int n;
        while ((n = zis.read(buf)) != -1)
          fos.write(buf, 0, n);
        fos.close();
      }
    }
    zis.close();
  }
}

