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

  private static final String ENV_VERSION = "1.7.0";
  private static final String ZIP_URL =
    "https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v" + ENV_VERSION + ".zip";
  private static final String PYTHON_PATH = "usr/bin/python3.13";
  private static final String LD_LIBRARY_PATH = "usr/lib64";
  private static final String FFMPEG_PATH = "usr/bin/ffmpeg";
  private static final String LD_SO_PATH = "lib64/ld-linux-aarch64.so.1";

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
    File py = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH);
    if (py.exists() && py.canExecute()) {
      appendLine("Окружение [" + ENV_VERSION + "] уже установлено и исполняемо.");
      runOnUiThread(() -> nextButton.setEnabled(true));
      return;
    }

    appendLine("Качаем yt-dlp [" + ENV_VERSION + "]... ~143Мб");
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
        File ldSoFile = new File(dst, LD_SO_PATH);
        File libDir = new File(dst, LD_LIBRARY_PATH);

        if (!pythonFile.exists()) {
          appendLine("Ошибка: Python binary не найден: " + pythonFile.getAbsolutePath());
          return;
        }
        if (!ffmpegFile.exists()) {
          appendLine("Ошибка: FFmpeg binary не найден: " + ffmpegFile.getAbsolutePath());
          return;
        }
        if (!ldSoFile.exists()) {
          appendLine("Ошибка: Линковщик не найден: " + ldSoFile.getAbsolutePath());
          return;
        }
        if (!libDir.exists() || !libDir.isDirectory()) {
          appendLine("Ошибка: Папка библиотек не найдена: " + libDir.getAbsolutePath());
          return;
        }

        // Установка прав на выполнение
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

        if (ldSoFile.setExecutable(true, false)) {
          appendLine("Линковщик теперь исполняемый: " + ldSoFile.getAbsolutePath());
        } else {
          appendLine("Ошибка: Не удалось установить права на выполнение для линковщика: " + ldSoFile.getAbsolutePath());
        }

        // Логирование содержимого usr/lib64
        File[] libFiles = libDir.listFiles();
        if (libFiles == null || libFiles.length == 0) {
          appendLine("Предупреждение: Папка " + libDir.getAbsolutePath() + " пуста!");
        } else {
          appendLine("Найдены библиотеки в " + libDir.getAbsolutePath() + ":");
          for (File lib : libFiles) {
            appendLine("  - " + lib.getName());
          }
        }

        appendLine("ytgui-env установлен!");
        runOnUiThread(() -> nextButton.setEnabled(true));
      } catch (Exception e) {
        appendLine("Ошибка установки: " + e.getMessage());
      }
    }).start();
  }

  private void runDownloader() {
    File py = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH);
    File ldSo = new File(getFilesDir(), "ytgui-env/" + LD_SO_PATH);
    File libDir = new File(getFilesDir(), "ytgui-env/" + LD_LIBRARY_PATH);

    if (!py.exists() || !py.canExecute()) {
      appendLine("Окружение [" + ENV_VERSION + "] повреждено или не исполняемо.\nТребуется переустановка.");
      return;
    }
    if (!ldSo.exists() || !ldSo.canExecute()) {
      appendLine("Линковщик повреждён или не исполняем: " + ldSo.getAbsolutePath() + "\nТребуется переустановка.");
      return;
    }
    if (!libDir.exists() || !libDir.isDirectory()) {
      appendLine("Ошибка: Папка библиотек не найдена: " + libDir.getAbsolutePath());
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

        // Проверка линковщика и Python
        ProcessBuilder testPb = new ProcessBuilder(ldSo.getAbsolutePath(), py.getAbsolutePath(), "--version");
        testPb.directory(env);
        testPb.environment().put("LD_LIBRARY_PATH",
          env.getAbsolutePath() + "/" + LD_LIBRARY_PATH + ":" + System.getenv("LD_LIBRARY_PATH"));
        Process testProc = testPb.start();
        StringBuilder testOutput = new StringBuilder();
        stream(testProc.getInputStream(), testOutput);
        stream(testProc.getErrorStream(), testOutput);
        int testCode = testProc.waitFor();
        if (testCode != 0) {
          appendLine("Ошибка проверки Python через линковщик: код " + testCode);
          appendLine("Вывод: " + testOutput.toString());
        } else {
          appendLine("Python версия: " + testOutput.toString());
        }

        Process proc = pb.start();
        stream(proc.getInputStream(), null);
        stream(proc.getErrorStream(), null);
        int code = proc.waitFor();

        appendLine("Код завершения yt-dlp: " + code);
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

  private void stream(InputStream s, StringBuilder output) throws IOException {
    BufferedReader r = new BufferedReader(new InputStreamReader(s));
    String line;
    while ((line = r.readLine()) != null) {
      appendLine(line);
      if (output != null) {
        output.append(line).append("\n");
      }
    }
  }

  private String[] buildCommand() {
    String ldSoPath = new File(getFilesDir(), "ytgui-env/" + LD_SO_PATH).getAbsolutePath();
    String binPath = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH).getAbsolutePath();
    String[] cmd = new String[options.length + 4];
    cmd[0] = ldSoPath;
    cmd[1] = binPath;
    cmd[2] = "-m";
    cmd[3] = "yt_dlp";
    System.arraycopy(options, 0, cmd, 4, options.length);
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

