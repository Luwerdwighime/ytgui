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

  private static final String ENV_VERSION = "1.4.0";
  private static final String ZIP_URL =
    "https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v" + ENV_VERSION + ".zip";

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
    appendLine("Качаем yt-dlp v" + ENV_VERSION + "... ~127Мб");
    new Thread(() -> {
      try {
        File zip = new File(getFilesDir(), "ytgui-env.zip");
        File envTarget = new File(getFilesDir(), "ytgui-env-" + ENV_VERSION);
        File envFinal = new File(getFilesDir(), "ytgui-env");

        HttpURLConnection conn = (HttpURLConnection) new URL(ZIP_URL).openConnection();
        conn.connect();
        InputStream in = new BufferedInputStream(conn.getInputStream());
        FileOutputStream out = new FileOutputStream(zip);
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close(); out.close();

        appendLine("Распаковка ytgui-env...");
        unzip(zip, envTarget);

        zip.delete();
        if (envFinal.exists()) envFinal.delete();
        envTarget.renameTo(envFinal);

        new File(envFinal, "bin/ffmpeg").setExecutable(true);
        new File(envFinal, "bin/python3.11").setExecutable(true);

        appendLine("ytgui-env установлен!");
        runOnUiThread(() -> nextButton.setEnabled(true));
      } catch (Exception e) {
        appendLine("Ошибка установки: " + e.getMessage());
      }
    }).start();
  }

  private void runDownloader() {
    File py = new File(getFilesDir(), "ytgui-env/bin/python3.11");
    if (!py.exists()) {
      appendLine("Окружение v" + ENV_VERSION + " повреждено.\nТребуется переустановка.");
      return;
    }

    new Thread(() -> {
      try {
        ProcessBuilder pb = new ProcessBuilder(buildCommand());
        File env = new File(getFilesDir(), "ytgui-env");

        pb.environment().put("PREFIX", env.getAbsolutePath());
        pb.environment().put("PATH", env.getAbsolutePath() + "/bin:" + System.getenv("PATH"));
        pb.environment().put("LD_LIBRARY_PATH",
          env.getAbsolutePath() + "/lib:" + System.getenv("LD_LIBRARY_PATH"));
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
    while ((line = r.readLine()) != null) appendLine(line);
  }

  private String[] buildCommand() {
    String binPath = new File(getFilesDir(), "ytgui-env/bin/python3.11").getAbsolutePath();
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

