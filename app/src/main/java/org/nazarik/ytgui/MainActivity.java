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

    // --- Константы для конфигурации окружения ---
    private static final String ENV_VERSION = "1.7.0"; // Версия скачиваемого окружения
    private static final String ZIP_URL =
            "https://github.com/Luwerdwighime/ytgui-env/archive/refs/tags/v" + ENV_VERSION + ".zip"; // URL для загрузки ZIP-архива
    private static final String PYTHON_PATH = "bin/python3.12"; // Путь к исполняемому файлу Python внутри распакованного окружения
    private static final String LD_LIBRARY_PATH = "lib"; // Путь к папке с динамическими библиотеками внутри распакованного окружения
    private static final String FFMPEG_PATH = "bin/ffmpeg"; // Путь к исполняемому файлу FFmpeg внутри распакованного окружения

    // --- Элементы пользовательского интерфейса ---
    private TextView consoleText; // Текстовое поле для вывода логов и сообщений
    private Button nextButton;    // Кнопка для перехода к следующему экрану
    private String[] options;     // Массив строк, содержащий аргументы для yt-dlp (если они были переданы)

    /**
     * Вызывается при первом создании активности (экрана).
     * Здесь происходит инициализация UI и запуск логики приложения.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Устанавливаем макет для этого экрана

        // Находим элементы UI по их ID
        consoleText = findViewById(R.id.consoleText);
        nextButton = findViewById(R.id.nextButton);
        nextButton.setEnabled(false); // Изначально кнопка "Далее" отключена

        // Получаем опции, если они были переданы из другой активности
        options = getIntent().getStringArrayExtra("options");

        // Если опций нет (т.е. приложение запускается в первый раз),
        // запускаем процесс установки окружения.
        // Иначе (если опции есть), сразу запускаем скачивание через yt-dlp.
        if (options == null) {
            installEnv();
        } else {
            runDownloader();
        }

        // Устанавливаем слушатель нажатий для кнопки "Далее"
        nextButton.setOnClickListener(v -> {
            // Создаем Intent для запуска новой активности (DownloadActivity)
            Intent intent = new Intent(this, DownloadActivity.class);
            startActivity(intent); // Запускаем DownloadActivity
        });
    }

    /**
     * Добавляет новую строку текста в консольное окно (TextView) и прокручивает его.
     * Этот метод должен вызываться из главного потока UI.
     * @param line Текст, который нужно добавить.
     */
    private void appendLine(String line) {
        // Выполняем действия в потоке UI, так как изменяем элементы UI
        runOnUiThread(() -> {
            consoleText.append(line + "\n"); // Добавляем текст с новой строки
            // Прокручиваем TextView до конца, чтобы всегда были видны последние сообщения
            int scroll = consoleText.getLineCount() * consoleText.getLineHeight();
            consoleText.scrollTo(0, scroll);
        });
    }

    /**
     * Проверяет, установлено ли окружение yt-dlp, и если нет, скачивает и распаковывает его.
     * Также устанавливает необходимые права на исполнение для бинарников.
     */
    private void installEnv() {
        // Путь к исполняемому файлу Python в нашей директории
        File py = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH);

        // Проверяем, существует ли Python и является ли он исполняемым.
        // Если да, то окружение уже установлено и готово к работе.
        if (py.exists() && py.canExecute()) {
            appendLine("Окружение [" + ENV_VERSION + "] уже установлено и исполняемо.");
            runOnUiThread(() -> nextButton.setEnabled(true)); // Активируем кнопку "Далее"
            return; // Выходим из метода
        }

        appendLine("Качаем yt-dlp [" + ENV_VERSION + "]... ~143Мб");
        // Запускаем процесс установки в новом потоке, чтобы не блокировать основной UI-поток
        new Thread(() -> {
            try {
                File zip = new File(getFilesDir(), "ytgui-env.zip"); // Временный файл для скачивания ZIP-архива

                // Устанавливаем HTTP-соединение для скачивания файла
                HttpURLConnection conn = (HttpURLConnection) new URL(ZIP_URL).openConnection();
                conn.connect(); // Открываем соединение

                // Читаем данные из входного потока (скачиваемый файл)
                InputStream in = new BufferedInputStream(conn.getInputStream());
                // Записываем данные в выходной поток (локальный ZIP-файл)
                FileOutputStream out = new FileOutputStream(zip);
                byte[] buf = new byte[4096]; // Буфер для чтения данных
                int n;
                // Читаем данные из InputStream и записываем в FileOutputStream
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                in.close();  // Закрываем входной поток
                out.close(); // Закрываем выходной поток

                appendLine("Распаковка ytgui-env...");
                unzip(zip, getFilesDir()); // Вызываем метод для распаковки ZIP-файла
                zip.delete(); // Удаляем временный ZIP-файл после распаковки

                // Переименовываем распакованную папку (например, "ytgui-env-1.7.0" в "ytgui-env")
                File src = new File(getFilesDir(), "ytgui-env-" + ENV_VERSION); // Исходная папка после распаковки
                File dst = new File(getFilesDir(), "ytgui-env");               // Целевая папка

                if (dst.exists()) { // Если целевая папка уже существует, удаляем её
                    appendLine("Удаляем существующую папку " + dst.getName() + "...");
                    deleteRecursive(dst); // Рекурсивное удаление папки
                }
                // Пытаемся переименовать исходную папку в целевую
                if (!src.renameTo(dst)) {
                    appendLine("Ошибка: Не удалось переименовать папку " + src.getName() + " в " + dst.getName());
                    throw new IOException("Failed to rename directory"); // Генерируем исключение при неудаче
                }
                appendLine("Папка переименована: " + src.getName() + " -> " + dst.getName());

                // --- Установка прав на выполнение для бинарников ---
                File pythonFile = new File(dst, PYTHON_PATH); // Путь к Python
                File ffmpegFile = new File(dst, FFMPEG_PATH); // Путь к FFmpeg
                File libDir = new File(dst, LD_LIBRARY_PATH); // Путь к папке с библиотеками

                // Проверяем, что необходимые бинарники существуют
                if (!pythonFile.exists()) {
                    appendLine("Ошибка: Python binary не найден: " + pythonFile.getAbsolutePath());
                    return;
                }
                if (!ffmpegFile.exists()) {
                    appendLine("Ошибка: FFmpeg binary не найден: " + ffmpegFile.getAbsolutePath());
                    return;
                }
                // Проверяем, что папка с библиотеками существует
                if (!libDir.exists() || !libDir.isDirectory()) {
                    appendLine("Ошибка: Папка библиотек не найдена: " + libDir.getAbsolutePath());
                    // Это может быть предупреждением, если бинарники статически скомпилированы
                    appendLine("Предупреждение: Отсутствие папки " + LD_LIBRARY_PATH + " может вызвать проблемы.");
                }

                // Устанавливаем права на выполнение (execute permissions) для Python
                // `setExecutable(true, false)` означает: сделать исполняемым, не для всех пользователей.
                if (pythonFile.setExecutable(true, false)) {
                    appendLine("Python binary теперь исполняемый: " + pythonFile.getAbsolutePath());
                } else {
                    appendLine("Ошибка: Не удалось установить права на выполнение для Python: " + pythonFile.getAbsolutePath());
                }

                // Устанавливаем права на выполнение для FFmpeg
                if (ffmpegFile.setExecutable(true, false)) {
                    appendLine("FFmpeg binary теперь исполняемый: " + ffmpegFile.getAbsolutePath());
                } else {
                    appendLine("Ошибка: Не удалось установить права на выполнение для FFmpeg: " + ffmpegFile.getAbsolutePath());
                }

                // Логируем содержимое папки с библиотеками (для отладки)
                if (libDir.exists() && libDir.isDirectory()) {
                    File[] libFiles = libDir.listFiles(); // Получаем список файлов в директории
                    if (libFiles == null || libFiles.length == 0) {
                        appendLine("Предупреждение: Папка " + libDir.getAbsolutePath() + " пуста!");
                    } else {
                        appendLine("Найдены библиотеки в " + libDir.getAbsolutePath() + ":");
                        for (File lib : libFiles) {
                            appendLine("  - " + lib.getName()); // Выводим имя каждой библиотеки
                        }
                    }
                }

                appendLine("ytgui-env установлен!");
                runOnUiThread(() -> nextButton.setEnabled(true)); // Активируем кнопку "Далее"
            } catch (Exception e) {
                appendLine("Ошибка установки: " + e.getMessage()); // Выводим сообщение об ошибке
                e.printStackTrace(); // Выводим полный стек вызовов для отладки в Logcat
            }
        }).start(); // Запускаем новый поток
    }

    /**
     * Запускает процесс yt-dlp с переданными опциями.
     * Проверяет целостность окружения перед запуском.
     */
    private void runDownloader() {
        // Путь к исполняемому файлу Python
        File py = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH);
        // Путь к папке с библиотеками
        File libDir = new File(getFilesDir(), "ytgui-env/" + LD_LIBRARY_PATH);

        // Проверяем, существует ли Python и является ли он исполняемым.
        if (!py.exists() || !py.canExecute()) {
            appendLine("Окружение [" + ENV_VERSION + "] повреждено или не исполняемо.\nТребуется переустановка.");
            runOnUiThread(() -> Toast.makeText(this, "Окружение повреждено, требуется переустановка", Toast.LENGTH_LONG).show());
            return;
        }
        // Проверяем, что папка с библиотеками существует
        if (!libDir.exists() || !libDir.isDirectory()) {
            appendLine("Ошибка: Папка библиотек не найдена: " + libDir.getAbsolutePath());
            appendLine("Предупреждение: Отсутствие папки " + LD_LIBRARY_PATH + " может вызвать проблемы.");
        }

        // Запускаем процесс yt-dlp в новом потоке, чтобы не блокировать UI
        new Thread(() -> {
            try {
                // Создаем ProcessBuilder для запуска Python
                // buildCommand() формирует массив аргументов для запуска Python и yt-dlp
                ProcessBuilder pb = new ProcessBuilder(buildCommand());
                File env = new File(getFilesDir(), "ytgui-env"); // Базовая директория нашего окружения

                // Устанавливаем рабочую директорию для процесса
                pb.directory(env);

                // --- Настройка переменных окружения для Bionic (системного линковщика Android) ---
                // Добавляем нашу папку 'bin' в PATH. Это позволяет системе найти 'python3.12' и 'ffmpeg'.
                pb.environment().put("PATH",
                        env.getAbsolutePath() + "/bin:" + // Наша папка bin
                                System.getenv("PATH")); // Существующий системный PATH

                // Добавляем нашу папку 'lib' в LD_LIBRARY_PATH. Это указывает Bionic,
                // где искать динамические библиотеки, необходимые для наших бинарников.
                pb.environment().put("LD_LIBRARY_PATH",
                        env.getAbsolutePath() + "/" + LD_LIBRARY_PATH + ":" + // Наша папка lib
                                System.getenv("LD_LIBRARY_PATH")); // Существующий системный LD_LIBRARY_PATH

                // Также можно установить PREFIX, хотя PATH и LD_LIBRARY_PATH обычно достаточно
                pb.environment().put("PREFIX", env.getAbsolutePath());

                // --- Запуск основного процесса yt-dlp ---
                appendLine("Запуск yt-dlp...");
                Process proc = pb.start(); // Запускаем основной процесс yt-dlp
                stream(proc.getInputStream(), null); // Перенаправляем стандартный вывод yt-dlp в консоль
                stream(proc.getErrorStream(), null); // Перенаправляем вывод ошибок yt-dlp в консоль
                int code = proc.waitFor(); // Ждем завершения процесса yt-dlp

                appendLine("Код завершения yt-dlp: " + code);
                if (code != 0) { // Если код завершения не равен 0, значит, была ошибка
                    appendLine("yt-dlp завершён с ошибкой.");
                    runOnUiThread(() ->
                            Toast.makeText(this, "Ошибка yt-dlp", Toast.LENGTH_LONG).show());
                }
                runOnUiThread(() -> nextButton.setEnabled(true)); // Активируем кнопку "Далее" после завершения
            } catch (Exception e) {
                appendLine("Ошибка запуска: " + e.getMessage()); // Выводим сообщение об ошибке
                e.printStackTrace(); // Выводим полный стек вызовов для отладки
                runOnUiThread(() ->
                        Toast.makeText(this, "Критическая ошибка запуска: " + e.getMessage(), Toast.LENGTH_LONG).show());
                runOnUiThread(() -> nextButton.setEnabled(true)); // Активируем кнопку при ошибке
            }
        }).start(); // Запускаем новый поток
    }

    /**
     * Читает данные из входного потока (например, вывод команды) и добавляет их в консоль.
     * Опционально может собирать вывод в StringBuilder.
     * @param s Входной поток (InputStream) для чтения.
     * @param output StringBuilder, в который нужно добавить вывод (может быть null).
     * @throws IOException Если произошла ошибка ввода/вывода.
     */
    private void stream(InputStream s, StringBuilder output) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(s)); // Создаем BufferedReader для построчного чтения
        String line;
        while ((line = r.readLine()) != null) { // Читаем строки, пока поток не закончится
            appendLine(line); // Добавляем строку в консоль UI
            if (output != null) { // Если передан StringBuilder, добавляем туда
                output.append(line).append("\n");
            }
        }
    }

    /**
     * Формирует массив строк, представляющий команду для запуска yt-dlp.
     * @return Массив строк с командой и её аргументами.
     */
    private String[] buildCommand() {
        // Получаем абсолютный путь к исполняемому файлу Python
        String binPath = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH).getAbsolutePath();

        // Создаем массив строк для команды.
        // cmd[0] = путь к python
        // cmd[1] = "-m" (флаг для запуска модуля python)
        // cmd[2] = "yt_dlp" (название модуля yt-dlp)
        // Остальные элементы - это переданные опции
        String[] cmd = new String[options.length + 3]; // +3, так как теперь есть binPath, "-m", "yt_dlp"
        cmd[0] = binPath;
        cmd[1] = "-m";
        cmd[2] = "yt_dlp";
        // Копируем пользовательские опции, начиная с 3-й позиции в массиве cmd
        System.arraycopy(options, 0, cmd, 3, options.length);
        return cmd;
    }

    /**
     * Распаковывает ZIP-файл в указанную целевую директорию.
     * @param zipFile Файл ZIP для распаковки.
     * @param targetDir Целевая директория, куда будут извлечены файлы.
     * @throws IOException Если произошла ошибка ввода/вывода.
     */
    private void unzip(File zipFile, File targetDir) throws IOException {
        ZipInputStream zis = null; // Поток для чтения ZIP-архива
        try {
            zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry entry;
            // Перебираем все записи (файлы/папки) в ZIP-архиве
            while ((entry = zis.getNextEntry()) != null) {
                File f = new File(targetDir, entry.getName()); // Создаем File-объект для текущей записи

                if (entry.isDirectory()) { // Если запись - это директория
                    f.mkdirs(); // Создаем все необходимые родительские директории
                } else { // Если запись - это файл
                    f.getParentFile().mkdirs(); // Убедимся, что родительские директории для файла существуют
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(f); // Открываем поток для записи файла
                        byte[] buf = new byte[4096]; // Буфер для чтения
                        int c;
                        // Читаем данные из ZIP-записи и записываем в файл
                        while ((c = zis.read(buf)) != -1) {
                            fos.write(buf, 0, c);
                        }
                    } finally {
                        if (fos != null) {
                            fos.close(); // Закрываем FileOutputStream в любом случае (даже при ошибке)
                        }
                    }
                }
                zis.closeEntry(); // Закрываем текущую запись ZIP-архива
            }
        } finally {
            if (zis != null) {
                zis.close(); // Закрываем ZipInputStream в любом случае
            }
        }
    }

    /**
     * Вспомогательная функция для рекурсивного удаления файла или директории.
     * @param fileOrDirectory Файл или директория для удаления.
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) { // Если это директория
            File[] children = fileOrDirectory.listFiles();
            if (children != null) { // Проверяем на null, так как listFiles может вернуть null
                for (File child : children) { // Перебираем все дочерние элементы
                    deleteRecursive(child); // Рекурсивно удаляем каждый дочерний элемент
                }
            }
        }
        fileOrDirectory.delete(); // Удаляем сам файл или (пустую) директорию
    }
}

