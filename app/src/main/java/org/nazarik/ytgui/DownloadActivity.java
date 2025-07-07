package org.nazarik.ytgui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import java.util.*;

public class DownloadActivity extends Activity {
  private EditText urlInput;
  private Button buttonVideo;
  private Button buttonAudio;
  private Button buttonPlaylistVideo;
  private Button buttonPlaylistAudio;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    urlInput = findViewById(R.id.urlInput);
    buttonVideo = findViewById(R.id.buttonVideo);
    buttonAudio = findViewById(R.id.buttonAudio);
    buttonPlaylistVideo = findViewById(R.id.buttonPlaylistVideo);
    buttonPlaylistAudio = findViewById(R.id.buttonPlaylistAudio);

    setupDownloadButton(buttonVideo, getVideoOptions(), urlInput);
    setupDownloadButton(buttonAudio, getAudioOptions(), urlInput);
    setupDownloadButton(buttonPlaylistVideo, getPlaylistVideoOptions(), urlInput);
    setupDownloadButton(buttonPlaylistAudio, getPlaylistAudioOptions(), urlInput);
  }

  private void setupDownloadButton(Button button, String[] options, EditText input) {
    button.setOnClickListener(v -> {
      String url = input.getText().toString().trim();
      if (url.isEmpty()) {
        Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
        return;
      }

      ArrayList<String> args = new ArrayList<>();
      if (options != null && options.length > 0) {
        Collections.addAll(args, options);
      }
      args.add(url); // URL всегда последний

      Intent intent = new Intent(this, MainActivity.class);
      intent.putExtra("options", args.toArray(new String[0]));
      startActivity(intent);
    });
  }

  private String[] getVideoOptions() {
    return new String[] {
      "--format", "bestvideo"
    };
  }

  private String[] getAudioOptions() {
    return new String[] {
      "--extract-audio",
      "--audio-format", "mp3"
    };
  }

  private String[] getPlaylistVideoOptions() {
    return new String[] {
      "--yes-playlist",
      "--format", "bestaudio+bestvideo"
    };
  }

  private String[] getPlaylistAudioOptions() {
    return new String[] {
      "--yes-playlist",
      "--format", "bestaudio",
      "--extract-audio",
      "--audio-format", "mp3"
    };
  }
}

