package org.nazarik.ytgui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class ConsoleActivity extends AppCompatActivity {
  private TextView consoleOutput;
  private Button backButton;
  private final Handler handler = new Handler(Looper.getMainLooper());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);

    // Инициализация UI
    consoleOutput = findViewById(R.id.consoleOutput);
    backButton = findViewById(R.id.backButton);
    consoleOutput.setMovementMethod(new ScrollingMovementMethod());

    // Обработчик кнопки "Назад"
    backButton.setOnClickListener(v -> {
      startActivity(new Intent(this, DownloadActivity.class));
      finish();
    });

    // Получение команды из Intent
    String command = getIntent().getStringExtra("command");
    if (command != null) {
      executeCommand(command);
    }
  }

  // Выполнение команды в окружении ytgui-env
  private void executeCommand(String command) {
    new Thread(() -> {
      try {
        File envDir = new File(getFilesDir() + "/ytgui-env");
        String[] cmd = {envDir + "/bin/python", "-c", "import sys; sys.path.append('" + envDir + "'); import yt_dlp; " + command};

        Process process = new ProcessBuilder(cmd)
          .directory(envDir)
          .start();

        // Захват stdout
        new Thread(() -> {
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
              String finalLine = line;
              handler.post(() -> appendConsole(finalLine));
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }).start();

        // Захват stderr
        new Thread(() -> {
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
              String finalLine = line;
              handler.post(() -> appendConsole(finalLine));
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }).start();

        // Ожидание завершения процесса
        process.waitFor();
        handler.post(() -> backButton.setEnabled(true));
      } catch (Exception e) {
        e.printStackTrace();
        handler.post(() -> appendConsole("Ошибка: " + e.getMessage()));
        handler.post(() -> backButton.setEnabled(true));
      }
    }).start();
  }

  // Добавление текста в консоль с прокруткой вниз
  private void appendConsole(String text) {
    consoleOutput.append(text + "\n");
    int scrollAmount = consoleOutput.getLayout().getLineTop(consoleOutput.getLineCount()) - consoleOutput.getHeight();
    if (scrollAmount > 0) {
      consoleOutput.scrollTo(0, scrollAmount);
    }
  }
}

