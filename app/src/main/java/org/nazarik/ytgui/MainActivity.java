package org.nazarik.ytgui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Главная активность приложения ytgui.
 * Отвечает за проверку разрешений, копирование файлов демона, запуск демона
 * и отображение веб-интерфейса в WebView.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "YtguiApp";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String DEMON_NAME = "ytguid";
    private static final String DEMON_URL = "http://127.0.0.1:27523/hello";

    private WebView webView;
    private Process demonProcess;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        setupWebView();
        checkPermissionsAndStart();
    }

    /**
     * Настраивает WebView для отображения веб-интерфейса демона.
     */
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        // Отключаем полосы прокрутки и скрываем адресную строку (она по умолчанию не видна)
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Web page loaded: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + errorCode + " - " + description + " at " + failingUrl);
                Toast.makeText(MainActivity.this, "Ошибка загрузки веб-страницы: " + description, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Проверяет необходимые разрешения и запрашивает их, если они не предоставлены.
     * Запускает последовательность операций после получения разрешений.
     */
    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Проверяем, нужна ли динамическая проверка
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Запрос разрешения на запись в хранилище.");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "Разрешение на запись в хранилище уже есть.");
                startApplicationFlow();
            }
        } else {
            // Для Android < M (API 23) разрешения даются при установке
            Log.d(TAG, "Версия Android < M, разрешения не требуются для динамического запроса.");
            startApplicationFlow();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Разрешение предоставлено!");
                Toast.makeText(this, getString(R.string.permission_granted_message), Toast.LENGTH_SHORT).show();
                startApplicationFlow();
            } else {
                Log.w(TAG, "Разрешение отклонено!");
                showPermissionDeniedDialog();
            }
        }
    }

    /**
     * Показывает диалог, если разрешение на хранилище было отклонено.
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_denied_message)
                .setPositiveButton(R.string.reload_app, (dialog, which) -> checkPermissionsAndStart())
                .setNegativeButton(R.string.exit_app, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Запускает основной поток выполнения приложения:
     * копирование файлов, запуск демона и загрузка WebView.
     */
    private void startApplicationFlow() {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Начинаем копирование ресурсов демона.");
                copyAssetsToInternalStorage("ytguid");
                copyAssetsToInternalStorage("www");
                Log.d(TAG, "Ресурсы демона скопированы. Запускаем демона.");
                startDemon();
                Log.d(TAG, "Демон запущен. Загружаем WebView.");
                runOnUiThread(() -> webView.loadUrl(DEMON_URL));
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при копировании ресурсов или запуске демона: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Рекурсивно копирует файлы и папки из assets в filesDir() приложения.
     *
     * @param path Путь к ресурсу в assets (например, "ytguid" или "ytguid/www").
     * @throws IOException Если произошла ошибка при чтении/записи файлов.
     */
    private void copyAssetsToInternalStorage(String path) throws IOException {
        String[] assetsList = getAssets().list(path);
        if (assetsList == null || assetsList.length == 0) {
            // Это файл, а не папка
            copyFileFromAssets(path);
        } else {
            // Это папка, создаем ее и копируем содержимое
            File destDir = new File(getFilesDir(), path);
            if (!destDir.exists()) {
                if (!destDir.mkdirs()) {
                    Log.e(TAG, "Не удалось создать директорию: " + destDir.getAbsolutePath());
                    throw new IOException("Failed to create directory: " + destDir.getAbsolutePath());
                }
            }
            for (String asset : assetsList) {
                copyAssetsToInternalStorage(path + File.separator + asset);
            }
        }
    }

    /**
     * Копирует один файл из assets в filesDir().
     *
     * @param assetPath Полный путь к файлу в assets.
     * @throws IOException Если произошла ошибка при чтении/записи файла.
     */
    private void copyFileFromAssets(String assetPath) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getAssets().open(assetPath);
            File outFile = new File(getFilesDir(), assetPath);
            File parentDir = outFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.e(TAG, "Не удалось создать родительскую директорию для файла: " + parentDir.getAbsolutePath());
                    throw new IOException("Failed to create parent directory for file: " + parentDir.getAbsolutePath());
                }
            }
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            Log.d(TAG, "Скопирован файл: " + assetPath + " в " + outFile.getAbsolutePath());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Ошибка закрытия InputStream: " + e.getMessage());
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "Ошибка закрытия OutputStream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Запускает демон ytguid как отдельный процесс.
     * Устанавливает права на исполнение для исполняемого файла.
     *
     * @throws IOException Если произошла ошибка при запуске процесса.
     */
    private void startDemon() throws IOException {
        File demonFile = new File(getFilesDir(), DEMON_NAME);
        if (!demonFile.exists()) {
            throw new IOException("Исполняемый файл демона не найден: " + demonFile.getAbsolutePath());
        }

        // Устанавливаем права на исполнение
        if (!demonFile.canExecute()) {
            if (!demonFile.setExecutable(true, false)) { // true = owner, false = everyone else
                Log.w(TAG, "Не удалось установить права на исполнение для " + demonFile.getAbsolutePath());
            } else {
                Log.d(TAG, "Установлены права на исполнение для " + demonFile.getAbsolutePath());
            }
        }

        // Путь к исполняемому файлу демона
        String demonPath = demonFile.getAbsolutePath();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(demonPath);
            // Устанавливаем рабочую директорию для демона
            processBuilder.directory(getFilesDir());
            // Перенаправляем stdout и stderr демона в Logcat для отладки
            processBuilder.redirectErrorStream(true); // Объединяет stdout и stderr

            Log.d(TAG, "Запуск демона: " + demonPath + " из " + getFilesDir().getAbsolutePath());
            demonProcess = processBuilder.start();

            // Читаем вывод демона (для отладки)
            executorService.execute(() -> {
                try (InputStream is = demonProcess.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        Log.i(TAG, "DEMON_OUTPUT: " + new String(buffer, 0, read).trim());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Ошибка чтения вывода демона: " + e.getMessage());
                }
            });

            Log.d(TAG, "Демон успешно запущен.");
        } catch (IOException e) {
            Log.e(TAG, "Не удалось запустить демон: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Убиваем процесс демона при уничтожении активности
        if (demonProcess != null) {
            Log.d(TAG, "Останавливаем процесс демона.");
            demonProcess.destroy(); // Отправляет SIGTERM
            try {
                demonProcess.waitFor(); // Ждем завершения процесса
                Log.d(TAG, "Процесс демона завершен.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Ожидание завершения демона прервано: " + e.getMessage());
            }
        }
        if (executorService != null) {
            executorService.shutdownNow(); // Останавливаем все задачи в пуле
        }
        Log.d(TAG, "MainActivity уничтожена.");
    }
}

