package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
public class ConsoleActivity extends AppCompatActivity {
  private TextView consoleOutput;
  private Button backButton;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);
    consoleOutput = findViewById(R.id.consoleOutput);
    backButton = findViewById(R.id.backButton);
    backButton.setEnabled(false);
    String command = getIntent().getStringExtra("command");
    new Thread(() -> runCommand(command)).start();
    backButton.setOnClickListener(v -> {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
      finish();
    });
  }
  private void runCommand(String command) {
    try {
      Process process = Runtime.getRuntime().exec(command);
      BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
      BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      String line;
      while ((line = stdout.readLine()) != null) {
        String finalLine = line;
        runOnUiThread(() -> consoleOutput.append(finalLine + "\n"));
      }
      while ((line = stderr.readLine()) != null) {
        String finalLine = line;
        runOnUiThread(() -> consoleOutput.append("ERR: " + finalLine + "\n"));
      }
      process.waitFor();
      runOnUiThread(() -> backButton.setEnabled(true));
    } catch (Exception e) {
      runOnUiThread(() -> consoleOutput.append("Error: " + e.getMessage() + "\n"));
      runOnUiThread(() -> backButton.setEnabled(true));
    }
  }
}

