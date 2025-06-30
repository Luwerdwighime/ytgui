package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;  // Добавлен импорт
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
    // Установка полного размера и копируемости
    consoleOutput.setLayoutParams(new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.MATCH_PARENT,
      1.0f));
    consoleOutput.setTextIsSelectable(true);
    // Ограничение высоты для кнопки
    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT,
      LinearLayout.LayoutParams.WRAP_CONTENT);
    buttonParams.setMargins(0, 0, 0, 16);
    backButton.setLayoutParams(buttonParams);
    // Получение команды
    String command = getIntent().getStringExtra("command");
    String[] env = null;
    // Запуск команды
    GitUtils.runCommand(this, command, consoleOutput, env, resultCode -> {
      // Установка результата для MainActivity
      setResult(resultCode);
      backButton.setEnabled(true);
    });
    // Настройка кнопки возврата
    backButton.setOnClickListener(v -> finish());
  }
}

