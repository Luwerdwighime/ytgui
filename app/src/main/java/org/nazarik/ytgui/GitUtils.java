package org.nazarik.ytgui;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
public class GitUtils {
  // Копирование файла из assets в files
  public static void copyFile(Context context, String assetPath, String outputPath) throws Exception {
    // Проверка существования актива
    try (InputStream in = context.getAssets().open(assetPath)) {
      // Проверка директории
      File outFile = new File(outputPath);
      File parentDir = outFile.getParentFile();
      if (!parentDir.exists() && !parentDir.mkdirs()) {
        Log.e("ytgui", "Failed to create directory: " + parentDir.getAbsolutePath());
        throw new Exception("Failed to create directory");
      }
      // Проверка прав записи
      if (!parentDir.canWrite()) {
        Log.e("ytgui", "Cannot write to directory: " + parentDir.getAbsolutePath());
        throw new Exception("Cannot write to directory");
      }
      // Копирование данных
      try (FileOutputStream out = new FileOutputStream(outFile)) {
        byte[] buffer = new byte[1024];
        int bytes;
        while ((bytes = in.read(buffer)) != -1) {
          out.write(buffer, 0, bytes);
        }
      }
      Log.d("ytgui", "Copied " + assetPath + " to " + outputPath);
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy " + assetPath + ": " + e.getMessage());
      throw e;
    }
  }
  // Копирование папки из assets в files с рекурсией
  public static void copyFolder(Context context, String assetDir, String outputDir) throws Exception {
    // Проверка существования актива
    String[] files = context.getAssets().list(assetDir);
    if (files == null) return;
    File outDir = new File(outputDir);
    if (!outDir.exists() && !outDir.mkdirs()) {
      Log.e("ytgui", "Failed to create directory: " + outputDir);
      throw new Exception("Failed to create directory");
    }
    for (String file : files) {
      String assetPath = assetDir + "/" + file;
      String outPath = outputDir + "/" + file;
      String[] subFiles = context.getAssets().list(assetPath);
      if (subFiles != null && subFiles.length > 0) {
        // Рекурсивно копируем подпапку
        copyFolder(context, assetPath, outPath);
      } else {
        // Копируем файл
        copyFile(context, assetPath, outPath);
      }
    }
  }
  // Выполнение команды с выводом в TextView или Logcat
  public static void runCommand(Context context, String command, TextView consoleOutput, String[] env, java.util.function.Consumer<Integer> onComplete) {
    new Thread(() -> {
      try {
        // Логирование команды
        Log.d("ytgui", "Executing command: " + command);
        // Запуск команды
        Process process = Runtime.getRuntime().exec(command, env);
        // Чтение stdout в потоке
        Thread stdoutThread = new Thread(() -> {
          try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = stdout.readLine()) != null) {
              String finalLine = line;
              if (consoleOutput != null) {
                ((AppCompatActivity) context).runOnUiThread(() -> consoleOutput.append(finalLine + "\n"));
              } else {
                Log.d("ytgui", "Command stdout: " + finalLine);
              }
            }
          } catch (Exception e) {
            Log.e("ytgui", "Failed to read stdout: " + e.getMessage());
          }
        });
        // Чтение stderr в потоке
        Thread stderrThread = new Thread(() -> {
          try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = stderr.readLine()) != null) {
              String finalLine = line;
              if (consoleOutput != null) {
                ((AppCompatActivity) context).runOnUiThread(() -> consoleOutput.append("ERR: " + finalLine + "\n"));
              } else {
                Log.e("ytgui", "Command stderr: " + finalLine);
              }
            }
          } catch (Exception e) {
            Log.e("ytgui", "Failed to read stderr: " + e.getMessage());
          }
        });
        // Запуск потоков чтения
        stdoutThread.start();
        stderrThread.start();
        // Ожидание завершения
        int exitCode = process.waitFor();
        stdoutThread.join();
        stderrThread.join();
        // Вызов коллбэка с кодом завершения
        if (consoleOutput != null) {
          ((AppCompatActivity) context).runOnUiThread(() -> {
            Button backButton = ((AppCompatActivity) context).findViewById(R.id.backButton);
            if (backButton != null) backButton.setEnabled(true);
            onComplete.accept(exitCode);
          });
        } else {
          onComplete.accept(exitCode);
        }
      } catch (Exception e) {
        // Логирование ошибки
        String error = e.getMessage();
        if (consoleOutput != null) {
          ((AppCompatActivity) context).runOnUiThread(() -> consoleOutput.append("Error: " + error + "\n"));
          ((AppCompatActivity) context).runOnUiThread(() -> {
            Button backButton = ((AppCompatActivity) context).findViewById(R.id.backButton);
            if (backButton != null) backButton.setEnabled(true);
            onComplete.accept(-1);
          });
        } else {
          Log.e("ytgui", "Command failed: " + error);
          onComplete.accept(-1);
        }
      }
    }).start();
  }
}

