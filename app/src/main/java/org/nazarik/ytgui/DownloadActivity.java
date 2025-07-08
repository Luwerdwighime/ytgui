// файл: app/src/main/java/org/nazarik/ytgui/DownloadActivity.java
package org.nazarik.ytgui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class DownloadActivity extends AppCompatActivity {
  private EditText urlInput;

  // флаги
  private boolean videoBestVideo = false;
  private boolean videoBestAudio = false;
  private boolean audioBestAudio = false;
  private boolean vplBestVideo = false;
  private boolean vplBestAudio = false;
  private boolean aplBestAudio = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    urlInput = findViewById(R.id.urlInput);
    Button pasteBtn = findViewById(R.id.pasteBtn);
    pasteBtn.setOnClickListener(v -> pasteClipboard());

    bindMainButton(R.id.btnVideo, R.id.gearVideo, true, true, false);
    bindMainButton(R.id.btnAudio, R.id.gearAudio, false, true, false);
    bindMainButton(R.id.btnVideoPlaylist, R.id.gearVideoPlaylist, true, true, true);
    bindMainButton(R.id.btnAudioPlaylist, R.id.gearAudioPlaylist, false, true, true);
  }

  private void pasteClipboard() {
    ClipboardManager cb = (ClipboardManager)
      getSystemService(Context.CLIPBOARD_SERVICE);
    if (cb != null && cb.hasPrimaryClip()) {
      ClipData clip = cb.getPrimaryClip();
      if (clip != null && clip.getItemCount() > 0) {
        urlInput.setText(clip.getItemAt(0).coerceToText(this));
      }
    }
  }

  private void bindMainButton(int btnId, int gearId,
      boolean allowBestVideo, boolean allowBestAudio, boolean isPlaylist) {

    Button actionBtn = findViewById(btnId);
    Button gearBtn = findViewById(gearId);

    actionBtn.setOnClickListener(v -> {
      String url = urlInput.getText().toString().trim();
      if (url.isEmpty()) {
        Toast.makeText(this,
          getString(R.string.toast_missing_url),
          Toast.LENGTH_SHORT).show();
        return;
      }

      ArrayList<String> opts = new ArrayList<>();
      if (isPlaylist) opts.add("--yes-playlist");

      // генерим -f bestvideo+bestaudio или один из них
      String format = null;
      if (btnId == R.id.btnVideo) {
        format = buildFormat(videoBestVideo, videoBestAudio);
      } else if (btnId == R.id.btnAudio) {
        format = buildFormat(false, audioBestAudio);
      } else if (btnId == R.id.btnVideoPlaylist) {
        format = buildFormat(vplBestVideo, vplBestAudio);
      } else if (btnId == R.id.btnAudioPlaylist) {
        format = buildFormat(false, aplBestAudio);
      }

      if (format != null) {
        opts.add("-f");
        opts.add(format);
      }

      opts.add(url);

      Intent intent = new Intent(this, MainActivity.class);
      intent.putStringArrayListExtra("options", opts);
      startActivity(intent);
    });

    gearBtn.setOnClickListener(v -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(getString(R.string.options_title));
      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(LinearLayout.VERTICAL);

      // bestvideo
      if (allowBestVideo) {
        CheckBox cbVideo = new CheckBox(this);
        cbVideo.setText(getString(R.string.options_bestvideo));
        cbVideo.setChecked(btnId == R.id.btnVideo
          ? videoBestVideo : vplBestVideo);
        cbVideo.setOnCheckedChangeListener((v2, checked) -> {
          if (btnId == R.id.btnVideo)
            videoBestVideo = checked;
          else
            vplBestVideo = checked;
        });
        layout.addView(cbVideo);
      }

      // bestaudio
      if (allowBestAudio) {
        CheckBox cbAudio = new CheckBox(this);
        cbAudio.setText(getString(R.string.options_bestaudio));
        cbAudio.setChecked(btnId == R.id.btnVideo
          ? videoBestAudio :
          btnId == R.id.btnAudio ? audioBestAudio :
          btnId == R.id.btnVideoPlaylist ? vplBestAudio :
          aplBestAudio);
        cbAudio.setOnCheckedChangeListener((v2, checked) -> {
          if (btnId == R.id.btnVideo)
            videoBestAudio = checked;
          else if (btnId == R.id.btnAudio)
            audioBestAudio = checked;
          else if (btnId == R.id.btnVideoPlaylist)
            vplBestAudio = checked;
          else
            aplBestAudio = checked;
        });
        layout.addView(cbAudio);
      }

      // предупреждение
      if (allowBestVideo && allowBestAudio) {
        TextView warn = new TextView(this);
        warn.setText(getString(R.string.options_warning));
        layout.addView(warn);
      }

      builder.setView(layout);
      builder.setPositiveButton("OK", null);
      builder.show();
    });
  }

  // постройка строки формата
  private String buildFormat(boolean hasVideo, boolean hasAudio) {
    if (hasVideo && hasAudio)
      return "bestvideo+bestaudio";
    if (hasVideo)
      return "bestvideo";
    if (hasAudio)
      return "bestaudio";
    return null;
  }
}

