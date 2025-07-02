package org.nazarik.ytgui;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Subproc {
  private final Context context;
  private final String executable;
  private final String[] options;
  private final String[] envVars;
  private TextView consoleOutput;

  public Subproc(Context context, String executable, String[] options, String[] envVars, TextView consoleOutput) {
    this.context = context;
    this.executable = executable;
    this.options = options != null ? options.clone() : new String[0];
    this.envVars = envVars != null ? envVars.clone() : new String[0];
    this.consoleOutput = consoleOutput;
  }

  public void run(java.util.function.Consumer<Integer> onComplete) {
    new Thread(() -> {
      try {
        File cmdFile = new File(executable);
        if (!cmdFile.exists() || !cmdFile.canExecute()) {
          Log.e("ytgui", "Command file not found or not executable: " + cmdFile.getAbsolutePath());
          throw new Exception("Command not executable");
        }
        List<String> commandList = new ArrayList<>();
        commandList.add(executable);
        for (String option : options) {
          if (!option.isEmpty()) {
            commandList.add(option);
          }
        }
        Log.d("ytgui", "Command list: " + String.join(" ", commandList));
        ProcessBuilder pb = new ProcessBuilder(commandList);
        for (String envVar : envVars) {
          String[] envPair = envVar.split("=", 2);
          if (envPair.length == 2) {
            pb.environment().put(envPair[0], envPair[1]);
            Log.d("ytgui", "Set env var: " + envPair[0] + "=" + envPair[1]);
          }
        }
        pb.directory(((AppCompatActivity) context).getFilesDir());
        Process process = pb.start();
        Log.d("ytgui", "Process started");
        // Чтение stdout и stderr в консоль
        Thread stdoutThread = new Thread(() -> {
          try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = stdout.readLine()) != null) {
              String finalLine = line;
              if (consoleOutput != null) {
                ((AppCompatActivity) context).runOnUiThread(() -> consoleOutput.append(finalLine + "\n"));
              }
              Log.d("ytgui", "STDOUT: " + finalLine);
            }
          } catch (Exception e) {
            Log.e("ytgui", "Failed to read stdout: " + e.getMessage());
          }
        });
        Thread stderrThread = new Thread(() -> {
          try (BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = stderr.readLine()) != null) {
              String finalLine = line;
              if (consoleOutput != null) {
                ((AppCompatActivity) context).runOnUiThread(() -> consoleOutput.append("ERR: " + finalLine + "\n"));
              }
              Log.e("ytgui", "STDERR: " + finalLine);
            }
          } catch (Exception e) {
            Log.e("ytgui", "Failed to read stderr: " + e.getMessage());
          }
        });
        stdoutThread.start();
        stderrThread.start();
        int exitCode = process.waitFor();
        stdoutThread.join();
        stderrThread.join();
        Log.d("ytgui", "Process finished with exit code: " + exitCode);
        if (consoleOutput != null) {
          ((AppCompatActivity) context).runOnUiThread(() -> {
            Button backButton = ((AppCompatActivity) context).findViewById(R.id.backButton);
            if (backButton != null) backButton.setEnabled(true);
            onComplete.accept(exitCode);
          });
        } else {
          onComplete.accept(exitCode);
        }
      } catch (Exception e) {
        Log.e("ytgui", "Command failed: " + e.getMessage(), e);
        if (consoleOutput != null) {
          ((AppCompatActivity) context).runOnUiThread(() -> consoleOutput.append("Error: " + e.getMessage() + "\n"));
          ((AppCompatActivity) context).runOnUiThread(() -> {
            Button backButton = ((AppCompatActivity) context).findViewById(R.id.backButton);
            if (backButton != null) backButton.setEnabled(true);
            onComplete.accept(-1);
          });
        } else {
          onComplete.accept(-1);
        }
      }
    }).start();
  }
}

