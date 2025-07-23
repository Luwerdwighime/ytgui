package org.nazarik.ytgui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class OptionsDialog extends Dialog {

  private DownloadActivity.DownloadType downloadType;
  private CheckBox bestVideoCheckbox, bestAudioCheckbox;
  private TextView warningTextView;
  private Button okButton;

  // Исходные состояния чекбоксов, устанавливаемые из DownloadActivity
  private boolean initialBestVideoChecked = false;
  private boolean initialBestAudioChecked = false;

  // Интерфейс для возврата выбранных опций
  public interface OnOptionsSelectedListener {
    void onOptionsSelected(boolean bestVideo, boolean bestAudio);
  }

  private OnOptionsSelectedListener listener;

  public OptionsDialog(@NonNull Context context, DownloadActivity.DownloadType type) {
    super(context);
    this.downloadType = type;
  }

  public void setOnOptionsSelectedListener(OnOptionsSelectedListener listener) {
    this.listener = listener;
  }

  public void setBestVideoChecked(boolean checked) {
    this.initialBestVideoChecked = checked;
  }

  public void setBestAudioChecked(boolean checked) {
    this.initialBestAudioChecked = checked;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_options); // Используем layout-файл

    // Заголовок диалога из strings.xml
    setTitle(getContext().getString(R.string.options_dialog_title));

    bestVideoCheckbox = findViewById(R.id.bestVideoCheckbox);
    bestAudioCheckbox = findViewById(R.id.bestAudioCheckbox);
    warningTextView = findViewById(R.id.warningTextView);
    okButton = findViewById(R.id.okButton);

    // Устанавливаем начальные состояния чекбоксов
    bestVideoCheckbox.setChecked(initialBestVideoChecked);
    bestAudioCheckbox.setChecked(initialBestAudioChecked);

    // Настраиваем видимость и текст в зависимости от типа загрузки
    switch (downloadType) {
      case VIDEO:
      case VIDEO_PLAYLIST:
        bestVideoCheckbox.setVisibility(View.VISIBLE);
        bestAudioCheckbox.setVisibility(View.VISIBLE);
        warningTextView.setVisibility(View.VISIBLE);
        break;
      case AUDIO:
      case AUDIO_PLAYLIST:
        bestVideoCheckbox.setVisibility(View.GONE); // Аудио не нужна опция bestvideo
        bestAudioCheckbox.setVisibility(View.VISIBLE);
        warningTextView.setVisibility(View.GONE); // Предупреждение не нужно для аудио
        break;
    }

    okButton.setOnClickListener(v -> {
      if (listener != null) {
        // В зависимости от типа, передаем правильные значения
        boolean selectedBestVideo = (downloadType == DownloadActivity.DownloadType.VIDEO ||
            downloadType == DownloadActivity.DownloadType.VIDEO_PLAYLIST) && bestVideoCheckbox.isChecked();
        boolean selectedBestAudio = bestAudioCheckbox.isChecked();
        listener.onOptionsSelected(selectedBestVideo, selectedBestAudio);
      }
      dismiss(); // Закрываем диалог
    });
  }
}


