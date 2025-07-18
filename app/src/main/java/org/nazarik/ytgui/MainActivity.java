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
    private static final String PYTHON_PATH = "bin/python3.12"; // Путь к исполняемому файлу Python
    private static final String LD_LIBRARY_PATH = "lib"; // Путь к папке с динамическими библиотеками

    // --- Элементы пользовательского интерфейса ---
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

        if (options == null) {
            installEnv();
        } else {
            runDownloader();
        }

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

    /**
     * Проверяет, установлено ли окружение, и если нет - скачивает и настраивает его.
     */
    private void installEnv() {
        File py = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH);

        // Если Python уже существует и исполняемый, ничего не делаем.
        if (py.exists() && py.canExecute()) {
            appendLine("Окружение [" + ENV_VERSION + "] уже установлено.");
            runOnUiThread(() -> nextButton.setEnabled(true));
            return;
        }

        appendLine("Качаем yt-dlp [" + ENV_VERSION + "]... ~143Мб");
        new Thread(() -> {
            try {
                // Скачивание
                File zip = new File(getFilesDir(), "ytgui-env.zip");
                HttpURLConnection conn = (HttpURLConnection) new URL(ZIP_URL).openConnection();
                conn.connect();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                FileOutputStream out = new FileOutputStream(zip);
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                in.close();
                out.close();

                // Распаковка
                appendLine("Распаковка ytgui-env...");
                unzip(zip, getFilesDir());
                zip.delete();

                // Переименование
                File src = new File(getFilesDir(), "ytgui-env-" + ENV_VERSION);
                File dst = new File(getFilesDir(), "ytgui-env");

                if (dst.exists()) {
                    deleteRecursive(dst);
                }
                if (!src.renameTo(dst)) {
                    throw new IOException("Не удалось переименовать " + src.getName() + " в " + dst.getName());
                }

                // Установка прав на исполнение для Python
                File pythonFile = new File(dst, PYTHON_PATH);
                if (!pythonFile.exists()) {
                    appendLine("Ошибка: Python binary не найден: " + pythonFile.getAbsolutePath());
                    return;
                }
                if (!pythonFile.setExecutable(true, false)) {
                    appendLine("Ошибка: Не удалось установить права на выполнение для Python.");
                }

                appendLine("ytgui-env установлен!");
                runOnUiThread(() -> nextButton.setEnabled(true));
            } catch (Exception e) {
                appendLine("Ошибка установки: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void runDownloader() {
        File py = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH);
        File libDir = new File(getFilesDir(), "ytgui-env/" + LD_LIBRARY_PATH);

        if (!py.exists() || !py.canExecute()) {
            appendLine("Окружение [" + ENV_VERSION + "] повреждено или не исполняемо.\nТребуется переустановка.");
            runOnUiThread(() -> Toast.makeText(this, "Окружение повреждено, требуется переустановка", Toast.LENGTH_LONG).show());
            return;
        }
        if (!libDir.exists() || !libDir.isDirectory()) {
            appendLine("Предупреждение: Папка библиотек не найдена и это может вызвать проблемы.");
        }

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(buildCommand());
                File env = new File(getFilesDir(), "ytgui-env");
                pb.directory(env);

                pb.environment().put("PATH",
                        env.getAbsolutePath() + "/bin:" + System.getenv("PATH"));
                pb.environment().put("LD_LIBRARY_PATH",
                        env.getAbsolutePath() + "/" + LD_LIBRARY_PATH + ":" + System.getenv("LD_LIBRARY_PATH"));
                pb.environment().put("PREFIX", env.getAbsolutePath());

                appendLine("Запуск yt-dlp...");
                Process proc = pb.start();
                stream(proc.getInputStream(), null);
                stream(proc.getErrorStream(), null);
                int code = proc.waitFor();

                appendLine("Код завершения yt-dlp: " + code);
                if (code != 0) {
                    appendLine("yt-dlp завершён с ошибкой.");
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка yt-dlp", Toast.LENGTH_LONG).show());
                }
                runOnUiThread(() -> nextButton.setEnabled(true));
            } catch (Exception e) {
                appendLine("Ошибка запуска: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Критическая ошибка запуска: " + e.getMessage(), Toast.LENGTH_LONG).show());
                runOnUiThread(() -> nextButton.setEnabled(true));
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
        String binPath = new File(getFilesDir(), "ytgui-env/" + PYTHON_PATH).getAbsolutePath();
        String[] cmd = new String[options.length + 3];
        cmd[0] = binPath;
        cmd[1] = "-m";
        cmd[2] = "yt_dlp";
        System.arraycopy(options, 0, cmd, 3, options.length);
        return cmd;
    }

    private void unzip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File f = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    f.mkdirs();
                } else {
                    f.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        byte[] buf = new byte[4096];
                        int c;
                        while ((c = zis.read(buf)) != -1) {
                            fos.write(buf, 0, c);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}

