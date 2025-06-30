package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
public class DownloadActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Инициализация UI
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);
    // Настройка элементов UI
    EditText urlInput = findViewById(R.id.urlInput);
    Button pasteButton = findViewById(R.id.pasteButton);
    Button downloadButton = findViewById(R.id.downloadButton);
    // Настройка кнопки вставки
    pasteButton.setOnClickListener(v -> {
      android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      if (clipboard.hasPrimaryClip()) {
        urlInput.setText(clipboard.getPrimaryClip().getItemAt(0).getText());
      }
    });
    // Настройка кнопки скачивания
    downloadButton.setOnClickListener(v -> {
      String url = urlInput.getText().toString();
      if (!url.isEmpty()) {
        String filesDir = getFilesDir().getAbsolutePath();
        String command = filesDir + "/git --version"; // Временная команда для теста
        Intent intent = new Intent(this, ConsoleActivity.class);
        intent.putExtra("command", command);
        startActivity(intent);
      }
    });
  }
}

