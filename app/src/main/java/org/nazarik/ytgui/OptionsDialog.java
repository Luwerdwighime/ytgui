package org.nazarik.ytgui;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OptionsDialog {

  public interface OnOptionsSelected {
    void onSelected(boolean bestVideo, boolean bestAudio);
  }

  public static void showVideoDialog(Context context, OnOptionsSelected listener) {
    CheckBox cbBestVideo = new CheckBox(context);
    cbBestVideo.setText("bestvideo");

    CheckBox cbBestAudio = new CheckBox(context);
    cbBestAudio.setText("bestaudio");

    TextView warning = new TextView(context);
    warning.setText("⚠️ bestvideo+bestaudio может качать по 2 файла");

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 40, 40, 40);
    layout.addView(cbBestVideo);
    layout.addView(cbBestAudio);
    layout.addView(warning);

    new AlertDialog.Builder(context)
      .setTitle("Опции")
      .setView(layout)
      .setPositiveButton("ОК", (dialog, which) ->
        listener.onSelected(cbBestVideo.isChecked(), cbBestAudio.isChecked()))
      .setNegativeButton("Отмена", null)
      .show();
  }

  public static void showAudioDialog(Context context, OnOptionsSelected listener) {
    CheckBox cbBestAudio = new CheckBox(context);
    cbBestAudio.setText("bestaudio");

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 40, 40, 40);
    layout.addView(cbBestAudio);

    new AlertDialog.Builder(context)
      .setTitle("Опции")
      .setView(layout)
      .setPositiveButton("ОК", (dialog, which) ->
        listener.onSelected(false, cbBestAudio.isChecked()))
      .setNegativeButton("Отмена", null)
      .show();
  }
}

