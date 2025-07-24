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

  /**
   * Вызывается при первом создании Activity.
   * Инициализирует пользовательский интерфейс, ActivityResultLauncher
   * и определяет режим работы (инициализация или закачка).
   *
   * @param savedInstanceState Если Activity повторно инициализируется после ранее закрытого состояния,
   * этот Bundle содержит данные, которые он предоставил в {@link #onSaveInstanceState}.
   * В противном случае это null.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    initializeUI();
    registerPermissionLauncher();
    handleIntentAndStartOperation();
  }

  /**
   * Инициализирует элементы пользовательского интерфейса.
   * Устанавливает движение прокрутки для consoleTextView и отключает nextButton по умолчанию.
   */
  private void initializeUI() {
    consoleTextView = findViewById(R.id.consoleTextView);
    consoleTextView.setMovementMethod(new ScrollingMovementMethod());
    nextButton = findViewById(R.id.nextButton);
    nextButton.setEnabled(false); // Кнопка неактивна по умолчанию

    nextButton.setOnClickListener(v -> navigateToDownloadActivity());
  }

  /**
   * Регистрирует {@link ActivityResultLauncher} для обработки результата запроса
   * разрешения MANAGE_APP_ALL_FILES_ACCESS_PERMISSION (для Android 11+).
   */
  private void registerPermissionLauncher() {
    manageStoragePermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> handleManageStoragePermissionResult()
    );
  }

  /**
   * Обрабатывает результат запроса разрешения MANAGE_EXTERNAL_STORAGE.
   * Если разрешение получено, запускает основную операцию, иначе отображает сообщение об ошибке.
   */
  private void handleManageStoragePermissionResult() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (Environment.isExternalStorageManager()) {
        appendLog("Разрешение на управление всеми файлами получено! ✅");
        startOperation(); // startOperation вызывается только после получения разрешения
      } else {
        appendLog("Разрешение на управление всеми файлами отклонено 😥");
        Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show();
        nextButton.setEnabled(false);
      }
    }
  }

  /**
   * Обрабатывает {@link Intent}, чтобы определить режим работы (закачка или инициализация).
   * В зависимости от режима, либо проверяет разрешения, либо сразу запускает операцию.
   */
  private void handleIntentAndStartOperation() {
    Intent intent = getIntent();
    if (intent != null && intent.hasExtra("options")) {
      ytDlpOptions = intent.getStringArrayListExtra("options"); // Получаем ArrayList
      // Режим закачки: сначала проверяем и запрашиваем разрешения
      // startOperation() будет вызван в checkAndRequestPermissions() или в onRequestPermissionsResult/manageStoragePermissionLauncher
      checkAndRequestPermissions();
    } else {
      // Режим инициализации: сразу запускаем операцию (разрешения на запись тут не нужны для скачивания окружения)
      startOperation();
    }
  }

  /**
   * Добавляет текст в консоль и прокручивает её вниз.
   * Вызывается на главном потоке.
   *
   * @param message Сообщение для добавления в консоль.
   */
  private void appendLog(final String message) {
    runOnUiThread(() -> {
      consoleTextView.append(message + "\n");
      // Прокрутка к последней строке только если Layout уже существует и текст есть
      if (consoleTextView.getLayout() != null) {
        final int scrollAmount = consoleTextView.getLayout().getLineTop(
            consoleTextView.getLineCount()) - consoleTextView.getHeight();
        if (scrollAmount > 0) {
          consoleTextView.scrollTo(0, scrollAmount);
        }
      }
    });
  }

  /**
   * Проверяет и запрашивает необходимые разрешения для доступа к хранилищу.
   * Для Android 11 (API 30) и выше запрашивает MANAGE_EXTERNAL_STORAGE.
   * Для Android 9 (API 28) и ниже запрашивает WRITE_EXTERNAL_STORAGE.
   */
  private void checkAndRequestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Для Android 11 (API 30) и выше
      if (!Environment.isExternalStorageManager()) {
        appendLog("Требуется разрешение на управление всеми файлами для сохранения контента. 📂");
        requestManageAllFilesAccessPermission();
      } else {
        appendLog("Разрешение на управление всеми файлами уже есть. ✅");
        startOperation(); // startOperation вызывается только после получения разрешения
      }
    } else {
      // Для Android 9 (API 28) и ниже
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED) {
        appendLog("Требуется разрешение на запись в хранилище для сохранения контента. 📝");
        requestLegacyWritePermission();
      } else {
        appendLog("Разрешение на запись в хранилище уже есть. ✅");
        startOperation(); // startOperation вызывается только после получения разрешения
      }
    }
  }

  /**
   * Рекурсивно удаляет файл или директорию.
   *
   * @param file Файл или директория для удаления.
   */
  private void deleteRecursive(File file) {
    if (!file.exists()) {
      return;
    }
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
   * Запускает процесс скачивания и настройки окружения yt-dlp в фоновом потоке.
   * Включает скачивание ZIP-архива, его распаковку, переименование папки
   * и установку битов выполнения для исполняемых файлов.
   */
  private void downloadAndSetupEnvironment() {
    executorService.execute(() -> {
      try {
        File filesDir = getFilesDir();
        File envDir = new File(filesDir, ENV_NAME);
        File pythonExecutable = new File(envDir, PYTHON_PATH);

        // Проверка: если окружение уже установлено и исполняемо, пропускаем установку
        if (isEnvironmentAlreadySetup(pythonExecutable)) {
          appendLog(getString(R.string.environment_already_installed, ENV_VERSION));
          runOnUiThread(() -> nextButton.setEnabled(true));
          return;
        }

        File zipFile = new File(filesDir, "v" + ENV_VERSION + ".zip");
        String downloadUrl = "https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v"
            + ENV_VERSION + ".zip";

        downloadZipFile(downloadUrl, zipFile);
        unzipEnvironment(zipFile, filesDir, envDir);
        setExecutablePermissions(envDir);

        appendLog(getString(R.string.environment_setup_complete));
        runOnUiThread(() -> nextButton.setEnabled(true));

      } catch (Exception e) {
        handleEnvironmentSetupError(e);
      }
    });
  }

  /**
   * Скачивает ZIP-файл по указанному URL в целевой файл.
   * Отслеживает и сообщает о прогрессе скачивания.
   *
   * @param downloadUrl URL для скачивания.
   * @param zipFile     Целевой файл для сохранения ZIP-архива.
   * @throws IOException Если произошла ошибка сети или файловой системы.
   */
  private void downloadZipFile(String downloadUrl, File zipFile) throws IOException {
    appendLog(getString(R.string.environment_download_start, ENV_VERSION, ZIP_SIZE));
    URL url = new URL(downloadUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.connect();

    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
      throw new IOException("HTTP error code: " + connection.getResponseCode());
    }

    int totalFileSize = connection.getContentLength();
    if (totalFileSize <= 0) {
      totalFileSize = ZIP_SIZE * 1024 * 1024; // Используем приблизительный размер
    }

    try (InputStream input = new BufferedInputStream(connection.getInputStream());
        OutputStream output = new FileOutputStream(zipFile)) {
      byte data[] = new byte[4096];
      long total = 0;
      int count;
      int lastProgress = 0;

      while ((count = input.read(data)) != -1) {
        total += count;
        output.write(data, 0, count);

        if (totalFileSize > 0) {
          int progress = (int) (total * 100 / totalFileSize);
          if (progress / 20 > lastProgress / 20 || (total >= totalFileSize && totalFileSize > 0) || progress == 100) {
            appendLog(getString(R.string.environment_download_progress, progress));
            lastProgress = progress;
          }
        }
      }
    } finally {
      connection.disconnect();
    }
    appendLog("Скачивание завершено. ✅");
  }

  /**
   * Выполняет команду yt-dlp в фоновом потоке, используя настроенное окружение.
   * Захватывает и отображает вывод stdout и stderr, обрабатывает код выхода.
   * После выполнения перемещает скачанные файлы.
   */
  private void executeYtDlp() {
    executorService.execute(() -> {
      File filesDir = getFilesDir();
      File envDir = new File(filesDir, ENV_NAME);
      File pythonExecutable = new File(envDir, PYTHON_PATH);
      File ffmpegExecutable = new File(envDir, FFMPEG_PATH);

      if (!isPythonEnvironmentValid(pythonExecutable)) {
        handleCorruptedEnvironment();
        return;
      }

      warnIfFfmpegNotFound(ffmpegExecutable);

      ProcessBuilder processBuilder = createYtDlpProcessBuilder(envDir, pythonExecutable);
      List<String> command = buildYtDlpCommand(pythonExecutable);

      Log.d("MainActivity", "Executing command: " + String.join(" ", command));
      appendLog("Запуск команды: " + String.join(" ", command) + "\n");

      StringBuilder errorOutput = new StringBuilder();
      try {
        Process process = processBuilder.command(command).start();
        startStreamReaders(process, errorOutput);
        int exitCode = process.waitFor();
        handleYtDlpExitCode(exitCode, errorOutput);
      } catch (IOException | InterruptedException e) {
        handleYtDlpExecutionError(e);
      } finally {
        runOnUiThread(() -> nextButton.setEnabled(true));
      }
    });
  }

  /**
   * Создает {@link ProcessBuilder} для выполнения команды yt-dlp.
   * Устанавливает рабочую директорию и переменные окружения.
   *
   * @param envDir           Директория окружения (ytgui-env).
   * @param pythonExecutable Исполняемый файл Python.
   * @return Сконфигурированный {@link ProcessBuilder}.
   */
  private ProcessBuilder createYtDlpProcessBuilder(File envDir, File pythonExecutable) {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(envDir); // Рабочая папка - ytgui-env

    // Устанавливаем переменные окружения
    processBuilder.environment().put("PREFIX", envDir.getAbsolutePath());
    processBuilder.environment().put("PATH", envDir.getAbsolutePath() + "/bin:" + System.getenv("PATH"));
    processBuilder.environment().put("LD_LIBRARY_PATH",
        new File(envDir, LD_LIBRARY_PATH).getAbsolutePath() + ":" + System.getenv("LD_LIBRARY_PATH"));
    return processBuilder;
  }

  /**
   * Формирует список аргументов команды для запуска yt-dlp.
   *
   * @param pythonExecutable Исполняемый файл Python.
   * @return Список строковых аргументов команды.
   */
  private List<String> buildYtDlpCommand(File pythonExecutable) {
    List<String> command = new ArrayList<>();
    command.add(pythonExecutable.getAbsolutePath());
    command.add("-m");
    command.add("yt_dlp");

    // Добавляем переданные опции
    if (ytDlpOptions != null) {
      command.addAll(ytDlpOptions);
    }
    return command;
  }

  /**
   * Запускает потоки для чтения stdout и stderr процесса.
   *
   * @param process     Запущенный процесс yt-dlp.
   * @param errorOutput {@link StringBuilder} для захвата вывода stderr.
   */
  private void startStreamReaders(Process process, StringBuilder errorOutput) {
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

    try {
      stdoutReader.join();
      stderrReader.join();
    } catch (InterruptedException e) {
      Log.e("MainActivity", "Stream readers interrupted: " + e.getMessage(), e);
      Thread.currentThread().interrupt(); // Восстановить статус прерывания
    }
  }

  /**
   * Обрабатывает код выхода процесса yt-dlp.
   * В случае успеха перемещает файлы, в случае ошибки выводит сообщение.
   *
   * @param exitCode    Код выхода процесса.
   * @param errorOutput Вывод stderr процесса.
   */
  private void handleYtDlpExitCode(int exitCode, StringBuilder errorOutput) {
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
  }

  /**
   * Обрабатывает ошибки, возникающие при выполнении команды yt-dlp.
   *
   * @param e Исключение.
   */
  private void handleYtDlpExecutionError(Exception e) {
    String errorMessage = String.format(getString(R.string.error_executing_yt_dlp), e.getMessage());
    appendLog("\n" + errorMessage);
    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    Log.e("MainActivity", "Error executing yt-dlp", e);
  }

  /**
   * Обрабатывает ошибки, возникающие при настройке окружения.
   *
   * @param e Исключение.
   */
  private void handleEnvironmentSetupError(Exception e) {
    Log.e("MainActivity", "Ошибка при настройке окружения: " + e.getMessage(), e);
    appendLog(getString(R.string.environment_setup_error, e.getMessage()) + " 😭");
    runOnUiThread(() -> nextButton.setEnabled(false));
  }

  /**
   * Проверяет, установлено ли окружение yt-dlp и является ли исполняемый файл Python действительным.
   *
   * @param pythonExecutable Файл исполняемого файла Python.
   * @return true, если окружение установлено и действительно, false в противном случае.
   */
  private boolean isEnvironmentAlreadySetup(File pythonExecutable) {
    return pythonExecutable.exists() && pythonExecutable.isFile() && pythonExecutable.canExecute();
  }

  /**
   * Проверяет, не повреждено ли Python-окружение (файл существует, является файлом и исполняем).
   *
   * @param pythonExecutable Файл исполняемого файла Python.
   * @return true, если Python-окружение действительно, false в противном случае.
   */
  private boolean isPythonEnvironmentValid(File pythonExecutable) {
    if (!pythonExecutable.exists() || !pythonExecutable.isFile() || !pythonExecutable.canExecute()) {
      appendLog(getString(R.string.environment_corrupted, ENV_VERSION));
      runOnUiThread(() -> Toast.makeText(this, R.string.environment_corrupted_toast, Toast.LENGTH_LONG).show());
      return false;
    }
    return true;
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
        ensureDestinationDirectoriesExist(documentsVideoDir, documentsAudioDir);
        moveContentFromTempDir(tempVideoDir, documentsVideoDir, "видеофайлов", "видеофайл/папка");
        moveContentFromTempDir(tempAudioDir, documentsAudioDir, "аудиофайлов", "аудиофайл/папка");
        appendLog("Перемещение файлов завершено! 🥳");
      } catch (IOException e) {
        appendLog("Ошибка при перемещении файлов: " + e.getMessage() + " 😭");
        Log.e("MainActivity", "Error moving downloaded files: " + e.getMessage(), e);
      }
    });
  }

  /**
   * Убеждается, что целевые директории для сохранения файлов существуют, и создает их при необходимости.
   *
   * @param videoDir Директория для видео.
   * @param audioDir Директория для аудио.
   * @throws IOException Если не удалось создать директории.
   */
  private void ensureDestinationDirectoriesExist(File videoDir, File audioDir) throws IOException {
    if (!videoDir.exists()) {
      if (!videoDir.mkdirs()) {
        throw new IOException("Не удалось создать целевую директорию: " + videoDir.getAbsolutePath());
      }
    }
    if (!audioDir.exists()) {
      if (!audioDir.mkdirs()) {
        throw new IOException("Не удалось создать целевую директорию: " + audioDir.getAbsolutePath());
      }
    }
  }

  /**
   * Перемещает содержимое из временной директории в целевую.
   *
   * @param tempDir        Временная директория.
   * @param destinationDir Целевая директория.
   * @param contentType    Тип контента (например, "видеофайлов").
   * @param fileType       Тип файла для логирования (например, "видеофайл/папка").
   * @throws IOException Если произошла ошибка при перемещении файлов.
   */
  private void moveContentFromTempDir(File tempDir, File destinationDir, String contentType, String fileType) throws IOException {
    if (tempDir.exists() && tempDir.isDirectory()) {
      appendLog("Перемещение " + contentType + " из " + tempDir.getName() + " в " + destinationDir.getAbsolutePath() + "...");
      File[] filesToMove = tempDir.listFiles();
      if (filesToMove != null) {
        for (File file : filesToMove) {
          moveFileOrDirectory(file, new File(destinationDir, file.getName()));
          appendLog("Перемещен " + fileType + ": " + file.getName() + " ✅");
        }
      }
      deleteRecursive(tempDir); // Удаляем временную папку после перемещения
      appendLog("Временная папка для " + contentType + " очищена. ✅");
    } else {
      appendLog("Временная папка для " + contentType + " не найдена или пуста: " + tempDir.getAbsolutePath());
    }
  }

  /**
   * Перемещает файл или директорию из одного места в другое.
   * Если {@code renameTo} не удается (например, на разных файловых системах),
   * выполняет копирование и последующее удаление исходного файла.
   *
   * @param source      Источник.
   * @param destination Цель.
   * @throws IOException Если произошла ошибка ввода-вывода.
   */
  private void moveFileOrDirectory(File source, File destination) throws IOException {
    if (!source.exists()) {
      return; // Если источника нет, ничего не делаем
    }

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
    } else {
      // Перемещаем файл
      if (!source.renameTo(destination)) {
        copyFile(source, destination); // Копируем, если renameTo не сработало
        if (!source.delete()) {
          Log.w("MainActivity", "Не удалось удалить исходный файл после копирования: " + source.getAbsolutePath());
        }
      }
    }
  }

  /**
   * Копирует содержимое одного файла в другой.
   *
   * @param sourceFile      Исходный файл.
   * @param destinationFile Целевой файл.
   * @throws IOException Если произошла ошибка ввода-вывода.
   */
  private void copyFile(File sourceFile, File destinationFile) throws IOException {
    try (InputStream in = new FileInputStream(sourceFile);
        OutputStream out = new FileOutputStream(destinationFile)) {
      byte[] buffer = new byte[4096];
      int length;
      while ((length = in.read(buffer)) != -1) {
        out.write(buffer, 0, length);
      }
    }
  }


  /**
   * Переходит на DownloadActivity и завершает текущую Activity.
   */
  private void navigateToDownloadActivity() {
    Intent nextIntent = new Intent(this, DownloadActivity.class);
    startActivity(nextIntent);
    finish(); // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
  }

  /**
   * Обрабатывает результат запроса разрешений, полученный через {@code onRequestPermissionsResult}.
   *
   * @param requestCode  Код запроса, переданный в {@code requestPermissions}.
   * @param permissions  Запрошенные разрешения.
   * @param grantResults Результаты предоставления соответствующих разрешений.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 1001) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        appendLog("Разрешение на запись в хранилище получено! ✅");
        startOperation(); // startOperation вызывается только после получения разрешения
      } else {
        appendLog("Разрешение на запись в хранилище отклонено 😥");
        Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show();
        nextButton.setEnabled(false);
      }
    }
  }

  /**
   * Запрашивает разрешение MANAGE_APP_ALL_FILES_ACCESS_PERMISSION для Android 11 (API 30) и выше.
   */
  private void requestManageAllFilesAccessPermission() {
    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
    Uri uri = Uri.fromParts("package", getPackageName(), null);
    intent.setData(uri);
    manageStoragePermissionLauncher.launch(intent);
  }

  /**
   * Запрашивает разрешение WRITE_EXTERNAL_STORAGE для Android 9 (API 28) и ниже.
   */
  private void requestLegacyWritePermission() {
    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
  }

  /**
   * Устанавливает биты выполнения для исполняемых файлов Python и FFmpeg.
   *
   * @param envDir Директория окружения.
   * @throws IOException Если не удалось установить бит выполнения или файл не найден.
   */
  private void setExecutablePermissions(File envDir) throws IOException {
    File pythonExecutable = new File(envDir, PYTHON_PATH);
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
  }

  /**
   * Запускает основную операцию MainActivity в зависимости от режима.
   * Либо начинает закачку, либо настраивает окружение.
   */
  private void startOperation() {
    if (ytDlpOptions != null) {
      // Режим закачки
      appendLog("Запуск yt-dlp... 🚀");
      executeYtDlp();
    } else {
      // Режим инициализации
      downloadAndSetupEnvironment();
    }
  }

  /**
   * Распаковывает ZIP-архив в указанную директорию и переименовывает извлеченную папку.
   * Отслеживает и сообщает о прогрессе распаковки.
   *
   * @param zipFile   Архивный файл.
   * @param filesDir  Базовая директория приложения.
   * @param envDir    Целевая директория для окружения.
   * @throws IOException Если произошла ошибка ввода-вывода.
   */
  private void unzipEnvironment(File zipFile, File filesDir, File envDir) throws IOException {
    appendLog(getString(R.string.environment_unzip_start));
    unzip(zipFile, filesDir); // Распаковываем ZIP

    // После распаковки ZIP-файл будет иметь структуру ytgui-env-1.7.0/
    File extractedDir = new File(filesDir, ENV_NAME + "-" + ENV_VERSION);

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
    appendLog("Распаковка завершена. ✅");
  }

  /**
   * Распаковывает ZIP-архив в указанную директорию.
   * Отслеживает и сообщает о прогрессе распаковки.
   *
   * @param zipFile   Архивный файл.
   * @param targetDir Целевая директория.
   * @throws IOException Если произошла ошибка ввода-вывода.
   */
  private void unzip(File zipFile, File targetDir) throws IOException {
    try (ZipFile zip = new ZipFile(zipFile)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      long totalBytes = 0;
      // Сначала посчитаем общий размер для прогресса распаковки
      List<ZipEntry> entryList = new ArrayList<>(); // Сохраняем записи, чтобы пройтись дважды
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        entryList.add(entry);
        if (!entry.isDirectory()) {
          totalBytes += entry.getSize();
        }
      }

      long extractedBytes = 0;
      int lastProgress = 0;

      for (ZipEntry entry : entryList) {
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
   * Выводит предупреждение в консоль, если исполняемый файл FFmpeg не найден.
   *
   * @param ffmpegExecutable Файл исполняемого файла FFmpeg.
   */
  private void warnIfFfmpegNotFound(File ffmpegExecutable) {
    if (!ffmpegExecutable.exists() || !ffmpegExecutable.isFile()) {
      appendLog(getString(R.string.ffmpeg_not_found_warning, ffmpegExecutable.getAbsolutePath()));
    }
  }

  /**
   * Вызывается, когда Activity завершается.
   * Завершает все фоновые потоки, чтобы избежать утечек памяти.
   */
  @Override
  protected void onDestroy() {
    super.onDestroy();
    executorService.shutdownNow(); // Завершаем потоки при уничтожении Activity
  }
}


