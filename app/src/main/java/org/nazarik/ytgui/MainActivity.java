package org.nazarik.ytgui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

  private static final String ENV_VERSION = "1.5.0";
  private static final String ZIP_URL =
    "https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v" + ENV_VERSION + ".zip";
  private static final String PYTHON_PATH = "ytgui-env/usr/bin/python3.13";
  private static final String LD_LIBRARY_PATH = "ytgui-env/usr/lib";
  private static final String FFMPEG_PATH = "ytgui-env/usr/bin/ffmpeg";

  private TextView consoleText;
  private Button nextButton;
  private String[] options;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    consoleText = findViewById(R.id.consoleText);
    nextButton = findViewById(R.id.nextButton);
    nextButton.setEnabled(false);

    options = getIntent().getStringArrayExtra("options");

    if (options == null) installEnv();
    else runDownloader();

    nextButton.setOnClickListener(v -> {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
    });
  }

  private void appendLine(String line) {
    runOnUiThread(() -> {
      consoleText.append(line + "\n");
      int scroll = consoleText.getLineCount() * consoleText.getLineHeight();
      consoleText.scrollTo(0, scroll);
    });
  }

  private void installEnv() {
    File py = new File(getFilesDir(), PYTHON_PATH);
    if (py.exists() && py.canExecute()) {
      appendLine("Окружение [" + ENV_VERSION + "] уже установлено и исполняемо.");
      runOnUiThread(() -> nextButton.setEnabled(true));
      return;
    }

    appendLine("Качаем yt-dlp [" + ENV_VERSION + "]... ~258Мб");
    new Thread(() -> {
      try {
        File zip = new File(getFilesDir(), "ytgui-env.zip");

        HttpURLConnection conn = (HttpURLConnection) new URL(ZIP_URL).openConnection();
        conn.connect();
        InputStream in = new BufferedInputStream(conn.getInputStream());
        FileOutputStream out = new FileOutputStream(zip);
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close(); out.close();

        appendLine("Распаковка ytgui-env...");
        unzip(zip, getFilesDir());

        zip.delete();

        // Переименование ytgui-env-1.5.0 → ytgui-env
        File src = new File(getFilesDir(), "ytgui-env-" + ENV_VERSION);
        File dst = new File(getFilesDir(), "ytgui-env");
        if (dst.exists()) dst.delete();
        src.renameTo(dst);

        // Установка прав на выполнение
        File pythonFile = new File(dst, PYTHON_PATH);
        File ffmpegFile = new File(dst, FFMPEG_PATH);

        if (!pythonFile.exists()) {
          appendLine("Ошибка: Python binary не найден: " + pythonFile.getAbsolutePath());
          return;
        }
        if (!ffmpegFile.exists()) {
          appendLine("Ошибка: FFmpeg binary не найден: " + ffmpegFile.getAbsolutePath());
          return;
        }

        if (pythonFile.setExecutable(true, false)) {
          appendLine("Python binary теперь исполняемый: " + pythonFile.getAbsolutePath());
        } else {
          appendLine("Ошибка: Не удалось установить права на выполнение для Python: " + pythonFile.getAbsolutePath());
        }

        if (ffmpegFile.setExecutable(true, false)) {
          appendLine("FFmpeg binary теперь исполняемый: " + ffmpegFile.getAbsolutePath());
        } else {
          appendLine("Ошибка: Не удалось установить права на выполнение для FFmpeg: " + ffmpegFile.getAbsolutePath());
        }

        appendLine("ytgui-env установлен!");
        runOnUiThread(() -> nextButton.setEnabled(true));
      } catch (Exception e) {
        appendLine("Ошибка установки: " + e.getMessage());
      }
    }).start();
  }

  private void runDownloader() {
    File py = new File(getFilesDir(), PYTHON_PATH);
    if (!py.exists() || !py.canExecute()) {
      appendLine("Окружение [" + ENV_VERSION + "] повреждено или не исполняемо.\nТребуется переустановка.");
      return;
    }

    new Thread(() -> {
      try {
        ProcessBuilder pb = new ProcessBuilder(buildCommand());
        File env = new File(getFilesDir(), "ytgui-env");

        pb.environment().put("PREFIX", env.getAbsolutePath());
        pb.environment().put("PATH", env.getAbsolutePath() + "/usr/bin:" + System.getenv("PATH"));
        pb.environment().put("LD_LIBRARY_PATH",
          env.getAbsolutePath() + "/" + LD_LIBRARY_PATH + ":" + System.getenv("LD_LIBRARY_PATH"));
        pb.directory(env);

        Process proc = pb.start();
        stream(proc.getInputStream());
        stream(proc.getErrorStream());
        int code = proc.waitFor();

        appendLine("Код завершения: " + code);
        if (code != 0) {
          appendLine("yt-dlp завершён с ошибкой.");
          runOnUiThread(() ->
            Toast.makeText(this, "Ошибка yt-dlp", Toast.LENGTH_LONG).show());
        }
        runOnUiThread(() -> nextButton.setEnabled(true));
      } catch (Exception e) {
        appendLine("Ошибка запуска: " + e.getMessage());
      }
    }).start();
  }

  private void stream(InputStream s) throws IOException {
    BufferedReader r = new BufferedReader(new InputStreamReader(s));
    String line;
    while ((line = r.readLine()) != null) {
      appendLine(line);
    }
  }

  private String[] buildCommand() {
    String binPath = new File(getFilesDir(), PYTHON_PATH).getAbsolutePath();
    String[] cmd = new String[options.length + 3];
    cmd[0] = binPath;
    cmd[1] = "-m";
    cmd[2] = "yt_dlp";
    System.arraycopy(options, 0, cmd, 3, options.length);
    return cmd;
  }

  private void unzip(File zipFile, File targetDir) throws IOException {
    ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
    ZipEntry entry;
    while ((entry = zis.getNextEntry()) != null) {
      File f = new File(targetDir, entry.getName());
      if (entry.isDirectory()) f.mkdirs();
      else {
        f.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buf = new byte[4096];
        int c;
        while ((c = zis.read(buf)) != -1) fos.write(buf, 0, c);
        fos.close();
      }
      zis.closeEntry();
    }
    zis.close();
  }
}

