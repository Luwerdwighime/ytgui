package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;

import java.util.ArrayList;

public class DownloadActivity extends Activity {

  private EditText urlInput;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    urlInput = findViewById(R.id.urlInput);
    Button pasteButton = findViewById(R.id.pasteButton);

    Button videoButton = findViewById(R.id.videoButton);
    Button audioButton = findViewById(R.id.audioButton);
    Button videoListButton = findViewById(R.id.videoListButton);
    Button audioListButton = findViewById(R.id.audioListButton);

    pasteButton.setOnClickListener(v -> {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      if (clipboard.hasPrimaryClip()) {
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
          String pasteData = clip.getItemAt(0).coerceToText(this).toString();
          urlInput.setText(pasteData);
        }
      }
    });

    Button.OnClickListener handler = btn -> {
      String url = urlInput.getText().toString().trim();
      if (url.isEmpty()) {
        Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
        return;
      }

      ArrayList<String> options = new ArrayList<>();

      // пока для всех кнопок --help, позже заменим на реальные команды
      options.add("--help");
      options.add(url);

      Intent intent = new Intent(this, ConsoleActivity.class);
      intent.putStringArrayListExtra("options", options);
      startActivity(intent);
    };

    videoButton.setOnClickListener(handler);
    audioButton.setOnClickListener(handler);
    videoListButton.setOnClickListener(handler);
    audioListButton.setOnClickListener(handler);
  }
}

