package org.nazarik.ytgui;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class ConsoleActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_console);
    String filesDir = getFilesDir().getAbsolutePath();
    String gitBinDir = filesDir + "/git-bin";
    String sshKeyPath = gitBinDir + "/ssh/id_rsa";
    String envDir = filesDir + "/ytgui-env";
    File sshKey = new File(sshKeyPath);
    if (!sshKey.exists()) {
      Log.e("ytgui", "SSH key not found at " + sshKeyPath);
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    String command = getIntent().getStringExtra("command");
    if (command == null) {
      Log.e("ytgui", "No command provided");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }
    try {
      GitUtils.runCommand(this, command, findViewById(R.id.consoleOutput), new String[]{"GIT_SSH_COMMAND=ssh -i " + sshKeyPath}, exitCode -> {
        if (exitCode == 0) {
          setResult(RESULT_OK);
        } else {
          setResult(RESULT_CANCELED);
        }
        finish();
      });
    } catch (Exception e) {
      Log.e("ytgui", "Exception during git clone", e);
      setResult(RESULT_CANCELED);
      finish();
    }
  }
}

