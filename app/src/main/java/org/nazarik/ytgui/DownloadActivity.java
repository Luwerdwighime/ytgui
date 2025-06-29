package org.nazarik.ytgui;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
public class DownloadActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);
    EditText urlInput = findViewById(R.id.urlInput);
    Button pasteButton = findViewById(R.id.pasteButton);
    Button downloadButton = findViewById(R.id.downloadButton);
    pasteButton.setOnClickListener(v -> {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      if (clipboard.hasPrimaryClip()) {
        urlInput.setText(clipboard.getPrimaryClip().getItemAt(0).getText());
      }
    });
    downloadButton.setOnClickListener(v -> {
      // Заглушка для будущей C++ логики
    });
  }
}

