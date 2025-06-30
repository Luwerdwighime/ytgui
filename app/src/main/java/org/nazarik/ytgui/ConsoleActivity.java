package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
public class ConsoleActivity extends AppCompatActivity {
  private TextView consoleOutput;
  private Button backButton;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Инициализация UI
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);
    // Настройка элементов UI
    consoleOutput = findViewById(R.id.consoleOutput);
    backButton = findViewById(R.id.backButton);
    backButton.setEnabled(false);
    // Получение команды и окружения
    String command = getIntent().getStringExtra("command");
    String[] env = getIntent().getStringArrayExtra("env");
    // Запуск команды
    GitUtils.runCommand(this, command, consoleOutput, env);
    // Настройка кнопки возврата
    backButton.setOnClickListener(v -> {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
      finish();
    });
  }
}

