// DownloadActivity.java
package org.nazarik.ytgui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

public class DownloadActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    EditText urlInput = findViewById(R.id.urlInput);
    Button pasteButton = findViewById(R.id.pasteButton);

    pasteButton.setOnClickListener(v -> {
      ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm.hasPrimaryClip()) {
        ClipData clip = cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
          urlInput.setText(clip.getItemAt(0).getText().toString());
        }
      }
    });

    setupDownloadButton(R.id.buttonVideo, new String[], urlInput);
    setupDownloadButton(R.id.buttonAudio, new String[]{"--help", "2"}, urlInput);
    setupDownloadButton(R.id.buttonPlaylistVideo, new String[]{"--help", "3"}, urlInput);
    setupDownloadButton(R.id.buttonPlaylistAudio, new String[]{"--help", "4"}, urlInput);
  }

  private void setupDownloadButton(int btnId, String[] flags, EditText urlInput) {
    Button btn = findViewById(btnId);
    btn.setOnClickListener(v -> {
      String url = urlInput.getText().toString();
      if (url.isEmpty()) {
        Toast.makeText(this, getString(R.string.toast_empty_url), Toast.LENGTH_SHORT).show();
        return;
      }
      String[] options = Stream.concat(Arrays.stream(flags), Stream.of(url))
        .toArray(String[]::new);
      Intent i = new Intent(this, MainActivity.class);
      i.putExtra("options", options);
      Toast.makeText(this, String.join(" ", options), Toast.LENGTH_LONG).show();
      startActivity(i);
    });
  }
}

