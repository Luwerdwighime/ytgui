package org.nazarik.ytgui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class DownloadActivity extends AppCompatActivity {
  private EditText urlInput;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    // Инициализация элементов UI
    urlInput = findViewById(R.id.urlInput);
    Button pasteButton = findViewById(R.id.pasteButton);
    Button videoButton = findViewById(R.id.videoButton);
    Button audioButton = findViewById(R.id.audioButton);
    Button videoPlaylistButton = findViewById(R.id.videoPlaylistButton);
    Button audioPlaylistButton = findViewById(R.id.audioPlaylistButton);

    // Обработчик кнопки вставки URL из буфера
    pasteButton.setOnClickListener(v -> {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      if (clipboard.hasPrimaryClip()) {
        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        urlInput.setText(item.getText());
      }
    });

    // Обработчики кнопок скачивания
    videoButton.setOnClickListener(v -> startConsoleActivity("yt-dlp --help " + urlInput.getText()));
    audioButton.setOnClickListener(v -> startConsoleActivity("yt-dlp --help " + urlInput.getText()));
    videoPlaylistButton.setOnClickListener(v -> startConsoleActivity("yt-dlp --help " + urlInput.getText()));
    audioPlaylistButton.setOnClickListener(v -> startConsoleActivity("yt-dlp --help " + urlInput.getText()));
  }

  // Запуск ConsoleActivity с передачей команды
  private void startConsoleActivity(String command) {
    Intent intent = new Intent(this, ConsoleActivity.class);
    intent.putExtra("command", command);
    startActivity(intent);
  }
}

