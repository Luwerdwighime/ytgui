package org.nazarik.ytgui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Диалог для выбора дополнительных опций загрузки (например, "лучшее видео" или "лучшее аудио").
 * Внешний вид и доступные опции зависят от {@link DownloadActivity.DownloadType}.
 */
public class OptionsDialog extends Dialog {

  // --- Тип загрузки, для которого предназначен диалог ---
  private DownloadActivity.DownloadType downloadType;

  // --- Элементы пользовательского интерфейса ---
  private CheckBox bestVideoCheckbox, bestAudioCheckbox;
  private TextView warningTextView;
  private Button okButton;

  // --- Исходные состояния чекбоксов, устанавливаемые из DownloadActivity ---
  private boolean initialBestVideoChecked = false;
  private boolean initialBestAudioChecked = false;

  // --- Слушатель для возврата выбранных опций ---
  private OnOptionsSelectedListener listener;

  /**
   * Интерфейс обратного вызова для уведомления о выбранных опциях.
   */
  public interface OnOptionsSelectedListener {
    /**
     * Вызывается, когда пользователь выбирает опции и нажимает кнопку "OK".
     *
     * @param bestVideo Флаг, указывающий, выбрана ли опция "лучшее видео".
     * @param bestAudio Флаг, указывающий, выбрана ли опция "лучшее аудио".
     */
    void onOptionsSelected(boolean bestVideo, boolean bestAudio);
  }

  /**
   * Конструктор для {@code OptionsDialog}.
   *
   * @param context Контекст, в котором создается диалог.
   * @param type    {@link DownloadActivity.DownloadType}, определяющий, какие опции будут доступны.
   */
  public OptionsDialog(@NonNull Context context, DownloadActivity.DownloadType type) {
    super(context);
    this.downloadType = type;
  }

  /**
   * Устанавливает слушатель для уведомления о выбранных опциях.
   *
   * @param listener Реализация {@link OnOptionsSelectedListener}.
   */
  public void setOnOptionsSelectedListener(OnOptionsSelectedListener listener) {
    this.listener = listener;
  }

  /**
   * Устанавливает начальное состояние для чекбокса "Лучшее видео".
   * Этот метод должен быть вызван до {@link #onCreate(Bundle)}.
   *
   * @param checked Исходное состояние (true, если выбрано, false в противном случае).
   */
  public void setBestVideoChecked(boolean checked) {
    this.initialBestVideoChecked = checked;
  }

  /**
   * Устанавливает начальное состояние для чекбокса "Лучшее аудио".
   * Этот метод должен быть вызван до {@link #onCreate(Bundle)}.
   *
   * @param checked Исходное состояние (true, если выбрано, false в противном случае).
   */
  public void setBestAudioChecked(boolean checked) {
    this.initialBestAudioChecked = checked;
  }

  /**
   * Вызывается при создании диалога.
   * Инициализирует View-элементы, устанавливает их начальные состояния
   * и настраивает видимость в зависимости от типа загрузки.
   *
   * @param savedInstanceState Если диалог повторно инициализируется после ранее закрытого состояния,
   * этот Bundle содержит данные, которые он предоставил в {@link #onSaveInstanceState}.
   * В противном случае это null.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_options);

    initializeDialogViews();
    applyInitialCheckboxStates();
    configureViewsBasedOnDownloadType();
    setupOkButtonListener();
  }

  /**
   * Инициализирует все View-элементы диалога, связывая их с соответствующими ID в макете.
   * Также устанавливает заголовок диалога.
   */
  private void initializeDialogViews() {
    setTitle(getContext().getString(R.string.options_dialog_title));
    bestVideoCheckbox = findViewById(R.id.bestVideoCheckbox);
    bestAudioCheckbox = findViewById(R.id.bestAudioCheckbox);
    warningTextView = findViewById(R.id.warningTextView);
    okButton = findViewById(R.id.okButton);
  }

  /**
   * Применяет начальные состояния чекбоксов, установленные из {@link DownloadActivity}.
   */
  private void applyInitialCheckboxStates() {
    bestVideoCheckbox.setChecked(initialBestVideoChecked);
    bestAudioCheckbox.setChecked(initialBestAudioChecked);
  }

  /**
   * Настраивает видимость чекбоксов и предупреждающего текста
   * в зависимости от {@link DownloadActivity.DownloadType}.
   */
  private void configureViewsBasedOnDownloadType() {
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
  }

  /**
   * Настраивает слушатель для кнопки "OK". При нажатии собирает выбранные опции
   * и передает их через слушатель, затем закрывает диалог.
   */
  private void setupOkButtonListener() {
    okButton.setOnClickListener(v -> {
      if (listener != null) {
        // В зависимости от типа загрузки, определяем, какие значения чекбоксов актуальны
        boolean selectedBestVideo = isVideoType() && bestVideoCheckbox.isChecked();
        boolean selectedBestAudio = bestAudioCheckbox.isChecked();
        listener.onOptionsSelected(selectedBestVideo, selectedBestAudio);
      }
      dismiss(); // Закрываем диалог
    });
  }

  /**
   * Вспомогательный метод для проверки, относится ли текущий тип загрузки к видео.
   *
   * @return true, если тип загрузки - видео или видео-плейлист, false в противном случае.
   */
  private boolean isVideoType() {
    return downloadType == DownloadActivity.DownloadType.VIDEO ||
           downloadType == DownloadActivity.DownloadType.VIDEO_PLAYLIST;
  }
}


