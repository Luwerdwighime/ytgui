package org.nazarik.ytgui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends AppCompatActivity {

  // --- Фундаментальные константы ---
  private static final String ENV_NAME = "ytgui-env";
  private static final String ENV_VERSION = "1.7.0"; // Версия скачиваемого окружения
  private static final String PYTHON_PATH = "bin/python3.12"; // Путь к исполняемому файлу Python
  private static final String FFMPEG_PATH = "bin/ffmpeg"; // Путь к исполняемому файлу FFmpeg
  private static final String LD_LIBRARY_PATH = "lib"; // Путь к папке с динамическими библиотеками
  private static final int ZIP_SIZE = 143; // Примерный размер ZIP-архива в мегабайтах

  // --- Элементы пользовательского интерфейса ---
  private TextView consoleTextView;
  private Button nextButton;
  private ArrayList<String> ytDlpOptions; // Используем ArrayList<String> для опций

  // Пул потоков для выполнения длительных операций в фоновом режиме
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  // Для запроса разрешения MANAGE_EXTERNAL_STORAGE
  private ActivityResultLauncher<Intent> manageStoragePermissionLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    consoleTextView = findViewById(R.id.consoleTextView);
    consoleTextView.setMovementMethod(new ScrollingMovementMethod());
    // Автоматическая прокрутка вниз при изменении текста
    consoleTextView.post(() -> {
      final int scrollAmount = consoleTextView.getLayout().getLineTop(
          consoleTextView.getLineCount()) - consoleTextView.getHeight();
      if (scrollAmount > 0) {
        consoleTextView.scrollTo(0, scrollAmount);
      }
    });

    nextButton = findViewById(R.id.nextButton);
    nextButton.setEnabled(false); // Кнопка неактивна по умолчанию

    // Инициализация ActivityResultLauncher для запроса разрешения MANAGE_EXTERNAL_STORAGE
    manageStoragePermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
              appendLog("Разрешение на управление всеми файлами получено! ✅");
              startOperation();
            } else {
              appendLog("Разрешение на управление всеми файлами отклонено 😥");
              Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show();
              nextButton.setEnabled(false);
            }
          }
        });

    // Получаем опции из Intent, если они есть (режим закачки)
    Intent intent = getIntent();
    if (intent != null && intent.hasExtra("options")) {
      ytDlpOptions = intent.getStringArrayListExtra("options"); // Получаем ArrayList
      // Режим закачки: сначала проверяем и запрашиваем разрешения
      checkAndRequestPermissions();
    } else {
      // Режим инициализации: сразу запускаем операцию
      startOperation();
    }

    nextButton.setOnClickListener(v -> {
      Intent nextIntent = new Intent(this, DownloadActivity.class);
      startActivity(nextIntent);
      finish(); // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
    });
  }

  /**
   * Проверяет и запрашивает необходимые разрешения.
   * Для Android 11 (API 30) и выше запрашивает MANAGE_EXTERNAL_STORAGE.
   * Для Android 9 (API 28) и ниже запрашивает WRITE_EXTERNAL_STORAGE.
   */
  private void checkAndRequestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Для Android 11 (API 30) и выше
      if (!Environment.isExternalStorageManager()) {
        appendLog("Требуется разрешение на управление всеми файлами для сохранения контента. 📂");
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        manageStoragePermissionLauncher.launch(intent);
      } else {
        appendLog("Разрешение на управление всеми файлами уже есть. ✅");
        startOperation();
      }
    } else {
      // Для Android 9 (API 28) и ниже
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED) {
        appendLog("Требуется разрешение на запись в хранилище для сохранения контента. 📝");
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
      } else {
        appendLog("Разрешение на запись в хранилище уже есть. ✅");
        startOperation();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 1001) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        appendLog("Разрешение на запись в хранилище получено! ✅");
        startOperation();
      } else {
        appendLog("Разрешение на запись в хранилище отклонено 😥");
        Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show();
        nextButton.setEnabled(false);
      }
    }
  }

  /**
   * Запускает основную операцию MainActivity в зависимости от режима.
   */
  private void startOperation() {
    if (ytDlpOptions != null) {
      // Режим закачки
      appendLog("Запуск yt-dlp... 🚀");
      executeYtDlp();
    } else {
      // Режим инициализации
      appendLog(getString(R.string.environment_download_start, ENV_VERSION, ZIP_SIZE));
      downloadAndSetupEnvironment();
    }
  }

  /**
   * Добавляет текст в консоль и прокручивает её вниз.
   * Вызывается на главном потоке.
   */
  private void appendLog(final String message) {
    runOnUiThread(() -> {
      consoleTextView.append(message + "\n");
      // Прокрутка к последней строке
      final int scrollAmount = consoleTextView.getLayout().getLineTop(
          consoleTextView.getLineCount()) - consoleTextView.getHeight();
      if (scrollAmount > 0) {
        consoleTextView.scrollTo(0, scrollAmount);
      }
    });
  }

  /**
   * Скачивает и настраивает окружение yt-dlp.
   */
  private void downloadAndSetupEnvironment() {
    executorService.execute(() -> {
      try {
        // Пути
        File filesDir = getFilesDir();
        File envDir = new File(filesDir, ENV_NAME);
        File zipFile = new File(filesDir, "v" + ENV_VERSION + ".zip");
        String downloadUrl = "https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v"
            + ENV_VERSION + ".zip";

        // Проверка: если окружение уже установлено и исполняемо, пропускаем установку
        File pythonExecutable = new File(envDir, PYTHON_PATH);
        if (pythonExecutable.exists() && pythonExecutable.isFile() && pythonExecutable.canExecute()) {
          appendLog(getString(R.string.environment_already_installed, ENV_VERSION));
          runOnUiThread(() -> nextButton.setEnabled(true));
          return;
        }

        // 1. Скачивание ZIP-архива
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
          throw new IOException("HTTP error code: " + connection.getResponseCode());
        }

        int totalFileSize = connection.getContentLength();
        if (totalFileSize <= 0) {
          // Если размер не указан, используем приблизительный из константы
          totalFileSize = ZIP_SIZE * 1024 * 1024;
        }

        InputStream input = new BufferedInputStream(connection.getInputStream());
        OutputStream output = new FileOutputStream(zipFile);

        byte data[] = new byte[4096];
        long total = 0;
        int count;
        int lastProgress = 0;

        while ((count = input.read(data)) != -1) {
          total += count;
          output.write(data, 0, count);

          if (totalFileSize > 0) {
            int progress = (int) (total * 100 / totalFileSize);
            // Отбиваем каждые 20% и последний процент последнего блока
            if (progress / 20 > lastProgress / 20 || (total >= totalFileSize && totalFileSize > 0) || progress == 100) {
              appendLog(getString(R.string.environment_download_progress, progress));
              lastProgress = progress;
            }
          }
        }
        output.flush();
        output.close();
        input.close();
        appendLog("Скачивание завершено. ✅");

        // 2. Распаковка ZIP-архива
        appendLog(getString(R.string.environment_unzip_start));
        unzip(zipFile, filesDir);
        appendLog("Распаковка завершена. ✅");

        // 3. Переименование и очистка
        File extractedDir = new File(filesDir, ENV_NAME + "-" + ENV_VERSION); // Правильное имя распакованной папки
        if (extractedDir.exists()) {
          if (envDir.exists()) {
            deleteRecursive(envDir); // Удаляем старую папку, если существует
          }
          if (!extractedDir.renameTo(envDir)) {
            throw new IOException("Не удалось переименовать папку " + extractedDir.getAbsolutePath()
                + " в " + envDir.getAbsolutePath());
          }
          appendLog("Папка окружения переименована. ✅");
        } else {
          throw new IOException("Распакованная папка не найдена: " + extractedDir.getAbsolutePath());
        }

        if (!zipFile.delete()) {
          Log.w("MainActivity", "Не удалось удалить ZIP-файл: " + zipFile.getAbsolutePath());
        }
        appendLog("Временные файлы удалены. ✅");

        // 4. Установка бита выполнения
        File ffmpegExecutable = new File(envDir, FFMPEG_PATH);

        if (pythonExecutable.exists() && pythonExecutable.isFile()) {
          if (!pythonExecutable.setExecutable(true, false)) {
            throw new IOException("Не удалось установить бит выполнения для Python.");
          }
        } else {
          throw new IOException("Файл Python не найден: " + pythonExecutable.getAbsolutePath());
        }

        if (ffmpegExecutable.exists() && ffmpegExecutable.isFile()) {
          if (!ffmpegExecutable.setExecutable(true, false)) {
            throw new IOException("Не удалось установить бит выполнения для FFmpeg.");
          }
        } else {
          appendLog(getString(R.string.ffmpeg_not_found_warning, ffmpegExecutable.getAbsolutePath()));
        }
        appendLog("Биты выполнения установлены. ✅");

        appendLog(getString(R.string.environment_setup_complete));
        runOnUiThread(() -> nextButton.setEnabled(true));

      } catch (Exception e) {
        Log.e("MainActivity", "Ошибка при настройке окружения: " + e.getMessage(), e);
        appendLog(getString(R.string.environment_setup_error, e.getMessage()) + " 😭");
        runOnUiThread(() -> nextButton.setEnabled(false));
      }
    });
  }

  /**
   * Распаковывает ZIP-архив в указанную директорию.
   *
   * @param zipFile Архивный файл.
   * @param targetDir Целевая директория.
   * @throws IOException Если произошла ошибка ввода-вывода.
   */
  private void unzip(File zipFile, File targetDir) throws IOException {
    try (ZipFile zip = new ZipFile(zipFile)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      long totalBytes = 0;
      // Сначала посчитаем общий размер для прогресса распаковки
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory()) {
          totalBytes += entry.getSize();
        }
      }

      entries = zip.entries(); // Сбросим итератор для повторного прохода
      long extractedBytes = 0;
      int lastProgress = 0;

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        File entryFile = new File(targetDir, entry.getName());

        if (entry.isDirectory()) {
          if (!entryFile.isDirectory() && !entryFile.mkdirs()) {
            throw new IOException("Не удалось создать директорию: " + entryFile.getAbsolutePath());
          }
        } else {
          // Убедимся, что родительские директории существуют
          File parent = entryFile.getParentFile();
          if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException(
                "Не удалось создать родительские директории для файла: " + entryFile.getAbsolutePath());
          }

          try (InputStream is = zip.getInputStream(entry); // Получаем InputStream из ZipFile
              FileOutputStream fos = new FileOutputStream(entryFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
              fos.write(buffer, 0, bytesRead);
              extractedBytes += bytesRead;

              if (totalBytes > 0) {
                int currentProgress = (int) (extractedBytes * 100 / totalBytes);
                // Отбиваем каждые 20% и последний процент последнего блока
                if (currentProgress / 20 > lastProgress / 20 || (extractedBytes == totalBytes && totalBytes > 0)) {
                  appendLog(getString(R.string.environment_unzip_progress, currentProgress));
                  lastProgress = currentProgress;
                }
              }
            }
          }
        }
      }
    }
  }


  /**
   * Рекурсивно удаляет файл или директорию.
   *
   * @param file Файл или директория для удаления.
   */
  private void deleteRecursive(File file) {
    if (!file.exists()) return;
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursive(child);
        }
      }
    }
    if (!file.delete()) {
      Log.w("MainActivity", "Не удалось удалить файл/директорию: " + file.getAbsolutePath());
    }
  }

  /**
   * Выполняет команду yt-dlp.
   */
  private void executeYtDlp() {
    executorService.execute(() -> {
      File filesDir = getFilesDir();
      File envDir = new File(filesDir, ENV_NAME);
      File pythonExecutable = new File(envDir, PYTHON_PATH);
      File ffmpegExecutable = new File(envDir, FFMPEG_PATH);

      // Проверяем, что Python-окружение не повреждено
      if (!pythonExecutable.exists() || !pythonExecutable.isFile() || !pythonExecutable.canExecute()) {
        appendLog(getString(R.string.environment_corrupted, ENV_VERSION));
        runOnUiThread(() -> {
          Toast.makeText(this, R.string.environment_corrupted_toast, Toast.LENGTH_LONG).show();
          nextButton.setEnabled(true);
        });
        return;
      }
      // Предупреждение, если FFmpeg не найден
      if (!ffmpegExecutable.exists() || !ffmpegExecutable.isFile()) {
        appendLog(getString(R.string.ffmpeg_not_found_warning, ffmpegExecutable.getAbsolutePath()));
      }

      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.directory(envDir); // Рабочая папка - ytgui-env

      // Устанавливаем переменные окружения
      processBuilder.environment().put("PREFIX", envDir.getAbsolutePath());
      processBuilder.environment().put("PATH", envDir.getAbsolutePath() + "/bin:" + System.getenv("PATH"));
      processBuilder.environment().put("LD_LIBRARY_PATH",
          new File(envDir, LD_LIBRARY_PATH).getAbsolutePath() + ":" + System.getenv("LD_LIBRARY_PATH"));

      List<String> command = new ArrayList<>();
      command.add(pythonExecutable.getAbsolutePath());
      command.add("-m");
      command.add("yt_dlp");

      // Добавляем переданные опции
      if (ytDlpOptions != null) {
        command.addAll(ytDlpOptions);
      }

      Log.d("MainActivity", "Executing command: " + String.join(" ", command));
      appendLog("Запуск команды: " + String.join(" ", command) + "\n");

      // Используем StringBuilder для захвата stderr, чтобы показать его в Toast
      StringBuilder errorOutput = new StringBuilder();

      try {
        Process process = processBuilder.command(command).start();

        // Поток для stdout
        Thread stdoutReader = new Thread(() -> {
          try (InputStream inputStream = process.getInputStream();
              BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
              appendLog(line);
            }
          } catch (IOException e) {
            Log.e("MainActivity", "Error reading stdout: " + e.getMessage(), e);
          }
        });

        // Поток для stderr
        Thread stderrReader = new Thread(() -> {
          try (InputStream errorStream = process.getErrorStream();
              BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
              appendLog(line);
              errorOutput.append(line).append("\n"); // Захватываем для Toast
            }
          } catch (IOException e) {
            Log.e("MainActivity", "Error reading stderr: " + e.getMessage(), e);
          }
        });

        stdoutReader.start();
        stderrReader.start();

        int exitCode = process.waitFor();

        stdoutReader.join();
        stderrReader.join();

        if (exitCode == 0) {
          appendLog("\nyt-dlp завершился успешно! ✅");
          Toast.makeText(this, R.string.download_complete_toast, Toast.LENGTH_LONG).show();
          // Перемещение файлов
          moveDownloadedFiles();
        } else {
          String lastErrorLine = errorOutput.length() > 0 ? errorOutput.toString().trim()
              .split("\n")[errorOutput.toString().trim().split("\n").length - 1] : "";
          String errorMessage = String.format(getString(R.string.error_executing_yt_dlp),
              "Код выхода " + exitCode + " (" + lastErrorLine + ")");
          appendLog("\n" + errorMessage);
          Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }

      } catch (IOException | InterruptedException e) {
        String errorMessage = String.format(getString(R.string.error_executing_yt_dlp), e.getMessage());
        appendLog("\n" + errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        Log.e("MainActivity", "Error executing yt-dlp", e);
      } finally {
        runOnUiThread(() -> nextButton.setEnabled(true));
      }
    });
  }

  /**
   * Перемещает скачанные файлы из временных папок (files/ytvideo и files/ytaudio)
   * в Documents/ytVideo и Documents/ytAudio соответственно.
   * Использует Scoped Storage.
   */
  private void moveDownloadedFiles() {
    executorService.execute(() -> {
      File filesDir = getFilesDir();

      // Целевые папки в Documents
      File documentsVideoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ytVideo");
      File documentsAudioDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ytAudio");

      // Временные папки, откуда скачивал yt-dlp
      File tempVideoDir = new File(filesDir, "ytvideo");
      File tempAudioDir = new File(filesDir, "ytaudio");

      try {
        // Убедимся, что целевые папки существуют
        if (!documentsVideoDir.exists()) {
          if (!documentsVideoDir.mkdirs()) {
            throw new IOException("Не удалось создать целевую директорию: " + documentsVideoDir.getAbsolutePath());
          }
        }
        if (!documentsAudioDir.exists()) {
          if (!documentsAudioDir.mkdirs()) {
            throw new IOException("Не удалось создать целевую директорию: " + documentsAudioDir.getAbsolutePath());
          }
        }

        // Перемещение содержимого из временной папки для видео
        if (tempVideoDir.exists() && tempVideoDir.isDirectory()) {
          appendLog("Перемещение видеофайлов из " + tempVideoDir.getName() + " в " + documentsVideoDir.getAbsolutePath() + "...");
          File[] videoFiles = tempVideoDir.listFiles();
          if (videoFiles != null) {
            for (File file : videoFiles) {
              moveFileOrDirectory(file, new File(documentsVideoDir, file.getName()));
              appendLog("Перемещен видеофайл/папка: " + file.getName() + " ✅");
            }
          }
          deleteRecursive(tempVideoDir); // Удаляем временную папку после перемещения
          appendLog("Временная папка для видео очищена. ✅");
        } else {
          appendLog("Временная папка для видео не найдена или пуста: " + tempVideoDir.getAbsolutePath());
        }

        // Перемещение содержимого из временной папки для аудио
        if (tempAudioDir.exists() && tempAudioDir.isDirectory()) {
          appendLog("Перемещение аудиофайлов из " + tempAudioDir.getName() + " в " + documentsAudioDir.getAbsolutePath() + "...");
          File[] audioFiles = tempAudioDir.listFiles();
          if (audioFiles != null) {
            for (File file : audioFiles) {
              moveFileOrDirectory(file, new File(documentsAudioDir, file.getName()));
              appendLog("Перемещен аудиофайл/папка: " + file.getName() + " ✅");
            }
          }
          deleteRecursive(tempAudioDir); // Удаляем временную папку после перемещения
          appendLog("Временная папка для аудио очищена. ✅");
        } else {
          appendLog("Временная папка для аудио не найдена или пуста: " + tempAudioDir.getAbsolutePath());
        }

        appendLog("Перемещение файлов завершено! 🥳");

      } catch (IOException e) {
        appendLog("Ошибка при перемещении файлов: " + e.getMessage() + " 😭");
        Log.e("MainActivity", "Error moving downloaded files: " + e.getMessage(), e);
      }
    });
  }


  /**
   * Перемещает файл или директорию из одного места в другое.
   *
   * @param source Источник.
   * @param destination Цель.
   * @throws IOException Если произошла ошибка ввода-вывода.
   */
  private void moveFileOrDirectory(File source, File destination) throws IOException {
    if (!source.exists()) return; // Если источника нет, ничего не делаем

    if (source.isDirectory()) {
      if (!destination.exists()) {
        if (!destination.mkdirs()) {
          throw new IOException("Не удалось создать целевую директорию: " + destination.getAbsolutePath());
        }
      }
      String[] children = source.list();
      if (children == null) { // Если директория пуста или нет прав
        Log.w("MainActivity", "Пустая или недоступная исходная директория: " + source.getAbsolutePath());
        return;
      }
      for (String child : children) {
        moveFileOrDirectory(new File(source, child), new File(destination, child));
      }
      // Не удаляем source здесь, так как это будет сделано в moveDownloadedFiles после обработки всех детей
    } else {
      // Перемещаем файл
      if (!source.renameTo(destination)) {
        // Если renameTo не сработало (например, на разных файловых системах), копируем и удаляем
        try (InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination)) {
          byte[] buffer = new byte[4096];
          int length;
          while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
          }
        }
        if (!source.delete()) {
          Log.w("MainActivity",
              "Не удалось удалить исходный файл после копирования: " + source.getAbsolutePath());
        }
      }
    }
  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    executorService.shutdownNow(); // Завершаем потоки при уничтожении Activity
  }
}

