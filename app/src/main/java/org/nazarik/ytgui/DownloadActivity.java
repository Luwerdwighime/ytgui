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
        opts = buildAudioPlaylistOptions(url, bestAudioAudioPl);
        break;
      default:
        opts = new String[]{url};
    }

    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra("options", opts);
    startActivity(intent);
  }

  private String[] buildVideoOptions(String url, boolean bv, boolean ba) {
    if (!bv && !ba) {
      return new String[] {
        "-o", "/storage/emulated/0/Documents/ytVideo/%(title)s.%(ext)s",
        url
      };
    }

    String format = bv && ba ? "bestvideo+bestaudio"
      : bv ? "bestvideo"
      : "bestaudio";

    return new String[] {
      "-f", format,
      "-o", "/storage/emulated/0/Documents/ytVideo/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildAudioOptions(String url, boolean ba) {
    if (!ba) {
      return new String[] {
        "-o", "/storage/emulated/0/Documents/ytAudio/%(title)s.%(ext)s",
        url
      };
    }

    return new String[] {
      "-f", "bestaudio",
      "-o", "/storage/emulated/0/Documents/ytAudio/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildVideoPlaylistOptions(String url, boolean bv, boolean ba) {
    if (!bv && !ba) {
      return new String[] {
        "--yes-playlist",
        "-o", "/storage/emulated/0/Documents/ytVideo/%(playlist)s/%(title)s.%(ext)s",
        url
      };
    }

    String format = bv && ba ? "bestvideo+bestaudio"
      : bv ? "bestvideo"
      : "bestaudio";

    return new String[] {
      "--yes-playlist",
      "-f", format,
      "-o", "/storage/emulated/0/Documents/ytVideo/%(playlist)s/%(title)s.%(ext)s",
      url
    };
  }

  private String[] buildAudioPlaylistOptions(String url, boolean ba) {
    if (!ba) {
      return new String[] {
        "--yes-playlist",
        "-o", "/storage/emulated/0/Documents/ytAudio/%(playlist)s/%(title)s.%(ext)s",
        url
      };
    }

    return new String[] {
      "--yes-playlist",
      "-f", "bestaudio",
      "-o", "/storage/emulated/0/Documents/ytAudio/%(playlist)s/%(title)s.%(ext)s",
      url
    };
  }
}

