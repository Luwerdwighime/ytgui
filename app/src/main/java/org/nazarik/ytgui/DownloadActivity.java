package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import java.util.*;

public class DownloadActivity extends Activity {
  private EditText urlInput;
  private Button buttonVideo;
  private Button buttonAudio;
  private Button buttonPlaylistVideo;
  private Button buttonPlaylistAudio;
  private Button buttonPaste;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    urlInput = findViewById(R.id.urlInput);
    buttonPaste = findViewById(R.id.buttonPaste);

    buttonVideo = findViewById(R.id.buttonVideo);
    buttonAudio = findViewById(R.id.buttonAudio);
    buttonPlaylistVideo = findViewById(R.id.buttonPlaylistVideo);
    buttonPlaylistAudio = findViewById(R.id.buttonPlaylistAudio);

    // Прямая привязка кнопки "Вставить"
    buttonPaste.setOnClickListener(v -> {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      if (clipboard.hasPrimaryClip()) {
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
          String text = clip.getItemAt(0).coerceToText(this).toString();
          urlInput.setText(text);
          urlInput.setSelection(text.length());
        }
      }
    });

    // Кнопки загрузки
    setupDownloadButton(buttonVideo, new String[] { "--format", "bestvideo" });
    setupDownloadButton(buttonAudio, new String[] {
      "--extract-audio", "--audio-format", "mp3"
    });
    setupDownloadButton(buttonPlaylistVideo, new String[] {
      "--yes-playlist", "--format", "bestaudio+bestvideo"
    });
    setupDownloadButton(buttonPlaylistAudio, new String[] {
      "--yes-playlist", "--format", "bestaudio",
      "--extract-audio", "--audio-format", "mp3"
    });
  }

  private void setupDownloadButton(Button button, String[] options) {
    button.setOnClickListener(v -> {
      String url = urlInput.getText().toString().trim();
      if (url.isEmpty()) {
        Toast.makeText(this, "Введите ссылку", Toast.LENGTH_SHORT).show();
        return;
      }

      ArrayList<String> args = new ArrayList<>(Arrays.asList(options));
      args.add(url);

      Intent intent = new Intent(this, MainActivity.class);
      intent.putExtra("options", args.toArray(new String[0]));
      startActivity(intent);
    });
  }
}

