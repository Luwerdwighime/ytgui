package org.nazarik.ytgui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DownloadActivity extends AppCompatActivity {

  private EditText urlInput;
  private Button pasteButton;

  // Кнопки
  private Button videoButton, audioButton, videoPlButton, audioPlButton;
  private Button videoGear, audioGear, videoPlGear, audioPlGear;

  // Флаги опций
  private boolean bestVideoVideo = false, bestAudioVideo = false;
  private boolean bestAudioAudio = false;
  private boolean bestVideoVideoPl = false, bestAudioVideoPl = false;
  private boolean bestAudioAudioPl = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    urlInput = findViewById(R.id.urlInput);
    pasteButton = findViewById(R.id.pasteButton);

    videoButton = findViewById(R.id.videoButton);
    audioButton = findViewById(R.id.audioButton);
    videoPlButton = findViewById(R.id.videoPlaylistButton);
    audioPlButton = findViewById(R.id.audioPlaylistButton);

    videoGear = findViewById(R.id.videoGear);
    audioGear = findViewById(R.id.audioGear);
    videoPlGear = findViewById(R.id.videoPlaylistGear);
    audioPlGear = findViewById(R.id.audioPlaylistGear);

    pasteButton.setOnClickListener(v -> {
      ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null && cm.getPrimaryClip() != null) {
        ClipData clip = cm.getPrimaryClip();
        if (clip.getItemCount() > 0) {
          urlInput.setText(clip.getItemAt(0).coerceToText(this));
        }
      }
    });

    // Шестерёнки
    videoGear.setOnClickListener(v ->
      OptionsDialog.showVideoDialog(this, (bv, ba) -> {
        bestVideoVideo = bv;
        bestAudioVideo = ba;
      })
    );

    audioGear.setOnClickListener(v ->
      OptionsDialog.showAudioDialog(this, (bv, ba) ->
        bestAudioAudio = ba
      )
    );

    videoPlGear.setOnClickListener(v ->
      OptionsDialog.showVideoDialog(this, (bv, ba) -> {
        bestVideoVideoPl = bv;
        bestAudioVideoPl = ba;
      })
    );

    audioPlGear.setOnClickListener(v ->
      OptionsDialog.showAudioDialog(this, (bv, ba) ->
        bestAudioAudioPl = ba
      )
    );

    // Кнопки запуска
    videoButton.setOnClickListener(v -> startDownload(0));
    audioButton.setOnClickListener(v -> startDownload(1));
    videoPlButton.setOnClickListener(v -> startDownload(2));
    audioPlButton.setOnClickListener(v -> startDownload(3));
  }

  private void startDownload(int type) {
    String url = urlInput.getText().toString().trim();
    if (url.isEmpty()) {
      Toast.makeText(this, R.string.toast_empty_url, Toast.LENGTH_SHORT).show();
      return;
    }

    String[] opts;
    switch (type) {
      case 0:
        opts = buildVideoOptions(url, bestVideoVideo, bestAudioVideo);
        break;
      case 1:
        opts = buildAudioOptions(url, bestAudioAudio);
        break;
      case 2:
        opts = buildVideoPlaylistOptions(url, bestVideoVideoPl, bestAudioVideoPl);
        break;
      case 3:
      default:
        opts = buildAudioPlaylistOptions(url, bestAudioAudioPl);
    }

    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra("options", opts);
    startActivity(intent);
  }

  private String[] buildVideoOptions(String url, boolean bestVideo, boolean bestAudio) {
    return new String[] {
      bestVideo ? "-f" : "",
      bestVideo && bestAudio ? "bestvideo+bestaudio" :
        bestVideo ? "bestvideo" :
        bestAudio ? "bestaudio" : "",
      "-o", "/storage/emulated/0/Documents/ytVideo/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildAudioOptions(String url, boolean bestAudio) {
    return new String[] {
      bestAudio ? "-f" : "",
      bestAudio ? "bestaudio" : "",
      "-o", "/storage/emulated/0/Documents/ytAudio/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildVideoPlaylistOptions(String url, boolean bestVideo, boolean bestAudio) {
    return new String[] {
      "--yes-playlist",
      bestVideo ? "-f" : "",
      bestVideo && bestAudio ? "bestvideo+bestaudio" :
        bestVideo ? "bestvideo" :
        bestAudio ? "bestaudio" : "",
      "-o", "/storage/emulated/0/Documents/ytVideo/%(playlist)s/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildAudioPlaylistOptions(String url, boolean bestAudio) {
    return new String[] {
      "--yes-playlist",
      bestAudio ? "-f" : "",
      bestAudio ? "bestaudio" : "",
      "-o", "/storage/emulated/0/Documents/ytAudio/%(playlist)s/%(title)s.%(ext)s",
      url
    };
  }
}

