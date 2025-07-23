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

public class DownloadActivity extends AppCompatActivity {

  private EditText urlEditText;
  private Button downloadVideoButton, downloadAudioButton, downloadVideoPlaylistButton,
      downloadAudioPlaylistButton;
  private ImageButton pasteButton;
  private ImageButton videoOptionsButton, audioOptionsButton, videoPlaylistOptionsButton,
      audioPlaylistOptionsButton;

  // Флаги для опций (для каждой кнопки свои)
  private boolean videoBestVideo = false;
  private boolean videoBestAudio = false;
  private boolean audioBestAudio = false;
  private boolean videoPlaylistBestVideo = false;
  private boolean videoPlaylistBestAudio = false;
  private boolean audioPlaylistBestAudio = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download);

    initViews();
    setupListeners();
  }

  /**
   * Инициализирует все View-элементы.
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
   * Настраивает слушателей для кнопок.
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
   * Вставляет текст из буфера обмена в поле ввода URL.
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
   * Запускает процесс скачивания, формируя опции для yt-dlp и передавая их в MainActivity.
   *
   * @param type Тип загрузки (видео, аудио, плейлист).
   */
  private void startDownload(DownloadType type) {
    String url = urlEditText.getText().toString().trim();
    if (TextUtils.isEmpty(url)) {
      Toast.makeText(this, R.string.url_missing_toast, Toast.LENGTH_SHORT).show();
      return;
    }

    ArrayList<String> options = new ArrayList<>();

    // Папки для временного сохранения внутри filesDir()
    // yt-dlp будет скачивать сюда, а MainActivity затем переместит в Documents.
    String videoTempPath = getFilesDir().getAbsolutePath() + "/ytvideo";
    String audioTempPath = getFilesDir().getAbsolutePath() + "/ytaudio";

    options.add("-o"); // Опция для выходного файла/директории
    // yt-dlp сам создаст поддиректорию с названием плейлиста, если указан %(playlist_title)s
    // MainActivity потом рекурсивно переместит все содержимое этих временных папок.

    switch (type) {
      case VIDEO:
        options.add(videoTempPath + "/%(title)s.%(ext)s");
        if (videoBestVideo && videoBestAudio) {
          options.add("-f");
          options.add("bestvideo+bestaudio");
        } else if (videoBestVideo) {
          options.add("-f");
          options.add("bestvideo");
        } else if (videoBestAudio) {
          options.add("-f");
          options.add("bestaudio");
          options.add("--extract-audio"); // Для bestaudio без bestvideo, извлекаем аудио
        }
        break;
      case AUDIO:
        options.add(audioTempPath + "/%(title)s.%(ext)s");
        options.add("-x"); // Извлечь аудио
        options.add("--audio-format"); // Формат аудио (mp3, aac, opus, vorbis, wav)
        options.add("mp3"); // По умолчанию mp3, можно дать юзеру выбор в диалоге
        if (audioBestAudio) {
          options.add("-f");
          options.add("bestaudio");
        }
        break;
      case VIDEO_PLAYLIST:
        options.add(videoTempPath + "/%(playlist_title)s/%(title)s.%(ext)s");
        options.add("--yes-playlist");
        if (videoPlaylistBestVideo && videoPlaylistBestAudio) {
          options.add("-f");
          options.add("bestvideo+bestaudio");
        } else if (videoPlaylistBestVideo) {
          options.add("-f");
          options.add("bestvideo");
        } else if (videoPlaylistBestAudio) {
          options.add("-f");
          options.add("bestaudio");
          options.add("--extract-audio");
        }
        break;
      case AUDIO_PLAYLIST:
        options.add(audioTempPath + "/%(playlist_title)s/%(title)s.%(ext)s");
        options.add("--yes-playlist");
        options.add("-x"); // Извлечь аудио
        options.add("--audio-format");
        options.add("mp3"); // По умолчанию mp3
        if (audioPlaylistBestAudio) {
          options.add("-f");
          options.add("bestaudio");
        }
        break;
    }

    options.add(url); // URL всегда последним

    Intent intent = new Intent(DownloadActivity.this, MainActivity.class);
    intent.putStringArrayListExtra("options", options); // Передаем ArrayList
    startActivity(intent);
    finish(); // Закрываем DownloadActivity
  }

  /**
   * Показывает диалог опций для выбранного типа загрузки.
   *
   * @param type Тип загрузки.
   */
  private void showOptionsDialog(DownloadType type) {
    OptionsDialog dialog = new OptionsDialog(this, type);

    // Устанавливаем текущие значения чекбоксов
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

    dialog.setOnOptionsSelectedListener((bestVideo, bestAudio) -> {
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
    });
    dialog.show();
  }

  /**
   * Перечисление для типов загрузки.
   */
  public enum DownloadType {
    VIDEO,
    AUDIO,
    VIDEO_PLAYLIST,
    AUDIO_PLAYLIST
  }
}


