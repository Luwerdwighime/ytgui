package org.nazarik.ytgui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DownloadActivity extends AppCompatActivity {

  // --- Элементы пользовательского интерфейса ---
  private EditText urlEditText;
  private Button downloadVideoButton, downloadAudioButton, downloadVideoPlaylistButton,
      downloadAudioPlaylistButton;
  private ImageButton pasteButton;
  private ImageButton videoOptionsButton, audioOptionsButton, videoPlaylistOptionsButton,
      audioPlaylistOptionsButton;

  // --- Флаги для опций загрузки ---
  // Каждый набор флагов соответствует определенной кнопке/типу загрузки.
  private boolean videoBestVideo = false;
  private boolean videoBestAudio = false;
  private boolean audioBestAudio = false;
  private boolean videoPlaylistBestVideo = false;
  private boolean videoPlaylistBestAudio = false;
  private boolean audioPlaylistBestAudio = false;

  /**
   * Вызывается при первом создании Activity.
   * Инициализирует View-элементы и настраивает слушателей событий.
   *
   * @param savedInstanceState Если Activity повторно инициализируется после ранее закрытого состояния,
   * этот Bundle содержит данные, которые он предоставил в {@link #onSaveInstanceState}.
   * В противном случае это null.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    initViews();
    setupListeners();
  }

  /**
   * Добавляет необходимые опции yt-dlp для загрузки аудио.
   *
   * @param options Список опций, в который будут добавлены новые опции.
   * @param isBestAudioChecked Флаг, указывающий, нужно ли выбирать лучшее аудио.
   */
  private void addAudioOptions(List<String> options, boolean isBestAudioChecked) {
    options.add("-x"); // Извлечь аудио
    options.add("--audio-format"); // Формат аудио (mp3, aac, opus, vorbis, wav)
    options.add("mp3"); // По умолчанию mp3, можно дать юзеру выбор в диалоге
    if (isBestAudioChecked) {
      options.add("-f");
      options.add("bestaudio");
    }
  }

  /**
   * Добавляет необходимые опции yt-dlp для загрузки видео/аудио комбинаций.
   *
   * @param options Список опций, в который будут добавлены новые опции.
   * @param isBestVideoChecked Флаг, указывающий, нужно ли выбирать лучшее видео.
   * @param isBestAudioChecked Флаг, указывающий, нужно ли выбирать лучшее аудио.
   */
  private void addVideoAudioOptions(List<String> options, boolean isBestVideoChecked, boolean isBestAudioChecked) {
    if (isBestVideoChecked && isBestAudioChecked) {
      options.add("-f");
      options.add("bestvideo+bestaudio");
    } else if (isBestVideoChecked) {
      options.add("-f");
      options.add("bestvideo");
    } else if (isBestAudioChecked) {
      options.add("-f");
      options.add("bestaudio");
      options.add("--extract-audio"); // Для bestaudio без bestvideo, извлекаем аудио
    }
  }

  /**
   * Инициализирует все View-элементы, связывая их с соответствующими ID в макете.
   */
  private void initViews() {
    urlEditText = findViewById(R.id.urlEditText);
    downloadVideoButton = findViewById(R.id.downloadVideoButton);
    downloadAudioButton = findViewById(R.id.downloadAudioButton);
    downloadVideoPlaylistButton = findViewById(R.id.downloadVideoPlaylistButton);
    downloadAudioPlaylistButton = findViewById(R.id.downloadAudioPlaylistButton);
    pasteButton = findViewById(R.id.pasteButton);

    videoOptionsButton = findViewById(R.id.videoOptionsButton);
    audioOptionsButton = findViewById(R.id.audioOptionsButton);
    videoPlaylistOptionsButton = findViewById(R.id.videoPlaylistOptionsButton);
    audioPlaylistOptionsButton = findViewById(R.id.audioPlaylistOptionsButton);
  }

  /**
   * Извлекает текст из буфера обмена и вставляет его в поле ввода URL.
   * Если буфер обмена пуст или не содержит текста, ничего не происходит.
   */
  private void pasteFromClipboard() {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null && clipboard.hasPrimaryClip()) {
      ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
      if (item != null && item.getText() != null) {
        urlEditText.setText(item.getText());
      }
    }
  }

  /**
   * Настраивает слушателей для всех кнопок загрузки и кнопок опций.
   */
  private void setupListeners() {
    pasteButton.setOnClickListener(v -> pasteFromClipboard());

    downloadVideoButton.setOnClickListener(v -> startDownload(DownloadType.VIDEO));
    downloadAudioButton.setOnClickListener(v -> startDownload(DownloadType.AUDIO));
    downloadVideoPlaylistButton.setOnClickListener(v -> startDownload(DownloadType.VIDEO_PLAYLIST));
    downloadAudioPlaylistButton.setOnClickListener(v -> startDownload(DownloadType.AUDIO_PLAYLIST));

    videoOptionsButton.setOnClickListener(v -> showOptionsDialog(DownloadType.VIDEO));
    audioOptionsButton.setOnClickListener(v -> showOptionsDialog(DownloadType.AUDIO));
    videoPlaylistOptionsButton.setOnClickListener(v -> showOptionsDialog(DownloadType.VIDEO_PLAYLIST));
    audioPlaylistOptionsButton.setOnClickListener(v -> showOptionsDialog(DownloadType.AUDIO_PLAYLIST));
  }

  /**
   * Показывает диалог опций для выбранного типа загрузки.
   * Устанавливает текущие значения чекбоксов в диалоге и обновляет флаги опций
   * после выбора пользователем.
   *
   * @param type Тип загрузки, для которого нужно показать диалог опций.
   */
  private void showOptionsDialog(DownloadType type) {
    OptionsDialog dialog = new OptionsDialog(this, type);
    setDialogInitialOptions(dialog, type); // Устанавливаем текущие значения чекбоксов
    dialog.setOnOptionsSelectedListener((bestVideo, bestAudio) -> updateDownloadOptions(type, bestVideo, bestAudio));
    dialog.show();
  }

  /**
   * Устанавливает начальные значения чекбоксов в диалоге опций
   * в зависимости от текущих флагов для данного типа загрузки.
   *
   * @param dialog Объект {@link OptionsDialog}.
   * @param type   Тип загрузки.
   */
  private void setDialogInitialOptions(OptionsDialog dialog, DownloadType type) {
    switch (type) {
      case VIDEO:
        dialog.setBestVideoChecked(videoBestVideo);
        dialog.setBestAudioChecked(videoBestAudio);
        break;
      case AUDIO:
        dialog.setBestAudioChecked(audioBestAudio);
        break;
      case VIDEO_PLAYLIST:
        dialog.setBestVideoChecked(videoPlaylistBestVideo);
        dialog.setBestAudioChecked(videoPlaylistBestAudio);
        break;
      case AUDIO_PLAYLIST:
        dialog.setBestAudioChecked(audioPlaylistBestAudio);
        break;
    }
  }

  /**
   * Обновляет флаги опций загрузки на основе выбора пользователя в диалоге.
   *
   * @param type      Тип загрузки, опции для которого были изменены.
   * @param bestVideo Значение для флага "лучшее видео".
   * @param bestAudio Значение для флага "лучшее аудио".
   */
  private void updateDownloadOptions(DownloadType type, boolean bestVideo, boolean bestAudio) {
    switch (type) {
      case VIDEO:
        videoBestVideo = bestVideo;
        videoBestAudio = bestAudio;
        break;
      case AUDIO:
        audioBestAudio = bestAudio;
        break;
      case VIDEO_PLAYLIST:
        videoPlaylistBestVideo = bestVideo;
        videoPlaylistBestAudio = bestAudio;
        break;
      case AUDIO_PLAYLIST:
        audioPlaylistBestAudio = bestAudio;
        break;
    }
  }

  /**
   * Запускает процесс скачивания.
   * Собирает URL из поля ввода, формирует список опций для yt-dlp
   * на основе выбранного типа загрузки и текущих флагов опций,
   * а затем передает эти опции в {@link MainActivity} для выполнения.
   *
   * @param type Тип загрузки (видео, аудио, плейлист видео, плейлист аудио).
   */
  private void startDownload(DownloadType type) {
    String url = urlEditText.getText().toString().trim();
    if (TextUtils.isEmpty(url)) {
      Toast.makeText(this, R.string.url_missing_toast, Toast.LENGTH_SHORT).show();
      return;
    }

    ArrayList<String> options = new ArrayList<>();
    addOutputDirectoryOption(options, type); // Добавляем опцию выходной директории
    addTypeSpecificOptions(options, type); // Добавляем опции, специфичные для типа загрузки
    options.add(url); // URL всегда последним

    Intent intent = new Intent(DownloadActivity.this, MainActivity.class);
    intent.putStringArrayListExtra("options", options); // Передаем ArrayList
    startActivity(intent);
    finish(); // Закрываем DownloadActivity
  }

  /**
   * Добавляет опцию выходной директории для yt-dlp в зависимости от типа загрузки.
   *
   * @param options Список опций.
   * @param type    Тип загрузки.
   */
  private void addOutputDirectoryOption(List<String> options, DownloadType type) {
    options.add("-o"); // Опция для выходного файла/директории
    String videoTempPath = getFilesDir().getAbsolutePath() + "/ytvideo";
    String audioTempPath = getFilesDir().getAbsolutePath() + "/ytaudio";

    switch (type) {
      case VIDEO:
        options.add(videoTempPath + "/%(title)s.%(ext)s");
        break;
      case AUDIO:
        options.add(audioTempPath + "/%(title)s.%(ext)s");
        break;
      case VIDEO_PLAYLIST:
        options.add(videoTempPath + "/%(playlist_title)s/%(title)s.%(ext)s");
        options.add("--yes-playlist"); // Указываем, что это плейлист
        break;
      case AUDIO_PLAYLIST:
        options.add(audioTempPath + "/%(playlist_title)s/%(title)s.%(ext)s");
        options.add("--yes-playlist"); // Указываем, что это плейлист
        break;
    }
  }

  /**
   * Добавляет опции yt-dlp, специфичные для выбранного типа загрузки.
   *
   * @param options Список опций.
   * @param type    Тип загрузки.
   */
  private void addTypeSpecificOptions(List<String> options, DownloadType type) {
    switch (type) {
      case VIDEO:
        addVideoAudioOptions(options, videoBestVideo, videoBestAudio);
        break;
      case AUDIO:
        addAudioOptions(options, audioBestAudio);
        break;
      case VIDEO_PLAYLIST:
        addVideoAudioOptions(options, videoPlaylistBestVideo, videoPlaylistBestAudio);
        break;
      case AUDIO_PLAYLIST:
        addAudioOptions(options, audioPlaylistBestAudio);
        break;
    }
  }

  /**
   * Перечисление для типов загрузки, используемых в приложении.
   */
  public enum DownloadType {
    VIDEO,
    AUDIO,
    VIDEO_PLAYLIST,
    AUDIO_PLAYLIST
  }
}


