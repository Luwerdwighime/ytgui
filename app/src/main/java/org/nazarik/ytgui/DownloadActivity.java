package org.nazarik.ytgui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DownloadActivity extends AppCompatActivity {

  private EditText urlInput;

  private boolean bestVideoVideo = false, bestAudioVideo = false;
  private boolean bestAudioAudio = false;
  private boolean bestVideoVideoPl = false, bestAudioVideoPl = false;
  private boolean bestAudioAudioPl = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    urlInput = findViewById(R.id.urlInput);

    findViewById(R.id.videoGear).setOnClickListener(v ->
      OptionsDialog.showVideoDialog(this, (bv, ba) -> {
        bestVideoVideo = bv;
        bestAudioVideo = ba;
      }));

    findViewById(R.id.audioGear).setOnClickListener(v ->
      OptionsDialog.showAudioDialog(this, (bv, ba) -> {
        bestAudioAudio = ba;
      }));

    findViewById(R.id.videoPlaylistGear).setOnClickListener(v ->
      OptionsDialog.showVideoDialog(this, (bv, ba) -> {
        bestVideoVideoPl = bv;
        bestAudioVideoPl = ba;
      }));

    findViewById(R.id.audioPlaylistGear).setOnClickListener(v ->
      OptionsDialog.showAudioDialog(this, (bv, ba) -> {
        bestAudioAudioPl = ba;
      }));

    findViewById(R.id.videoButton).setOnClickListener(v -> startDownload(0));
    findViewById(R.id.audioButton).setOnClickListener(v -> startDownload(1));
    findViewById(R.id.videoPlaylistButton).setOnClickListener(v -> startDownload(2));
    findViewById(R.id.audioPlaylistButton).setOnClickListener(v -> startDownload(3));
  }

  public void onPasteClicked(View v) {
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm != null && cm.getPrimaryClip() != null) {
      ClipData clip = cm.getPrimaryClip();
      if (clip.getItemCount() > 0) {
        urlInput.setText(clip.getItemAt(0).coerceToText(this));
      }
    }
  }

  private void startDownload(int type) {
    String url = urlInput.getText().toString().trim();
    if (url.isEmpty()) {
      Toast.makeText(this, "URL не может быть пустым", Toast.LENGTH_SHORT).show();
      return;
    }

    // Получаем надёжный путь к внутренней директории files
    String cachePath = getFilesDir().getAbsolutePath();

    String[] opts;
    switch (type) {
      case 0:
        opts = buildVideoOptions(url, bestVideoVideo, bestAudioVideo, cachePath);
        break;
      case 1:
        opts = buildAudioOptions(url, bestAudioAudio, cachePath);
        break;
      case 2:
        opts = buildVideoPlaylistOptions(url, bestVideoVideoPl, bestAudioVideoPl, cachePath);
        break;
      case 3:
        opts = buildAudioPlaylistOptions(url, bestAudioAudioPl, cachePath);
        break;
      default:
        // На всякий случай, если будет вызван с неправильным типом
        opts = new String[]{ "--cache-dir", cachePath, url };
    }

    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra("options", opts);
    startActivity(intent);
  }

  private String[] buildVideoOptions(String url, boolean bv, boolean ba, String cachePath) {
    if (!bv && !ba) {
      return new String[]{
        "--cache-dir", cachePath,
        "-o", "/storage/emulated/0/Documents/ytVideo/%(title)s.%(ext)s",
        url
      };
    }

    String format = bv && ba ? "bestvideo+bestaudio"
      : bv ? "bestvideo"
      : "bestaudio";

    return new String[]{
      "--cache-dir", cachePath,
      "-f", format,
      "-o", "/storage/emulated/0/Documents/ytVideo/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildAudioOptions(String url, boolean ba, String cachePath) {
    if (!ba) {
      return new String[]{
        "--cache-dir", cachePath,
        "-o", "/storage/emulated/0/Documents/ytAudio/%(title)s.%(ext)s",
        url
      };
    }

    return new String[]{
      "--cache-dir", cachePath,
      "-f", "bestaudio",
      "-o", "/storage/emulated/0/Documents/ytAudio/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildVideoPlaylistOptions(String url, boolean bv, boolean ba, String cachePath) {
    if (!bv && !ba) {
      return new String[]{
        "--cache-dir", cachePath,
        "--yes-playlist",
        "-o", "/storage/emulated/0/Documents/ytVideo/%(playlist)s/%(title)s.%(ext)s",
        url
      };
    }

    String format = bv && ba ? "bestvideo+bestaudio"
      : bv ? "bestvideo"
      : "bestaudio";

    return new String[]{
      "--cache-dir", cachePath,
      "--yes-playlist",
      "-f", format,
      "-o", "/storage/emulated/0/Documents/ytVideo/%(playlist)s/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildAudioPlaylistOptions(String url, boolean ba, String cachePath) {
    if (!ba) {
      return new String[]{
        "--cache-dir", cachePath,
        "--yes-playlist",
        "-o", "/storage/emulated/0/Documents/ytAudio/%(playlist)s/%(title)s.%(ext)s",
        url
      };
    }

    return new String[]{
      "--cache-dir", cachePath,
      "--yes-playlist",
      "-f", "bestaudio",
      "-o", "/storage/emulated/0/Documents/ytAudio/%(playlist)s/%(title)s.%(ext)s",
      url
    };
  }
}


