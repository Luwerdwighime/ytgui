package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Button;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import java.io.*;
import java.util.*;

public class ConsoleActivity extends Activity {

  private TextView consoleOutput;
  private ScrollView consoleScroll;
  private Button backButton;

  private final List<String> stderrBuffer = Collections.synchronizedList(new ArrayList<>());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);

    consoleOutput = findViewById(R.id.consoleOutput);
    consoleScroll = findViewById(R.id.consoleScroll);
    backButton = findViewById(R.id.backButton);

    backButton.setEnabled(false);
    backButton.setOnClickListener(v -> {
      startActivity(new Intent(this, DownloadActivity.class));
      finish();
    });

    ArrayList<String> options = getIntent().getStringArrayListExtra("options");
    runYtdlp(options);
  }

  private void runYtdlp(List<String> options) {
    new Thread(() -> {
      int exitCode = -1;

      try {
        File envDir = new File(getFilesDir(), "ytgui-env");
        File pythonExe = new File(envDir, "bin/python");

        List<String> args = new ArrayList<>();
        args.add(pythonExe.getAbsolutePath());
        args.add("-m");
        args.add("yt_dlp");
        args.addAll(options);

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(envDir);
        Process process = pb.start();

        // stdout
        new Thread(() -> {
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
              appendLine(line + "\n");
            }
          } catch (Exception e) {
            appendLine("[stdout error] " + e.getMessage() + "\n");
          }
        }).start();

        // stderr
        new Thread(() -> {
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
              stderrBuffer.add(line); // сохраняем для итогового сообщения
              appendLine("[stderr] " + line + "\n");
            }
          } catch (Exception e) {
            appendLine("[stderr error] " + e.getMessage() + "\n");
          }
        }).start();

        exitCode = process.waitFor();

      } catch (Exception e) {
        appendLine("Ошибка запуска: " + e.getMessage() + "\n");
      } finally {
        final int resultCode = exitCode;
        runOnUiThread(() -> {
          backButton.setEnabled(true);

          if (resultCode != 0) {
            String lastError = stderrBuffer.isEmpty() ? "Неизвестная ошибка" : stderrBuffer.get(stderrBuffer.size() - 1);
            String msg = "⚠ yt-dlp завершился с ошибкой (" + resultCode + "): " + lastError;
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
          }
        });
      }
    }).start();
  }

  private void appendLine(String text) {
    runOnUiThread(() -> {
      consoleOutput.append(text);
      consoleScroll.post(() -> consoleScroll.fullScroll(View.FOCUS_DOWN));
    });
  }
}

