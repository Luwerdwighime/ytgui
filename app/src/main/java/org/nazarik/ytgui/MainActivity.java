package org.nazarik.ytgui;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
public class MainActivity extends AppCompatActivity {
  static {
    System.loadLibrary("ytgui");
  }
  public native void copyFile(String assetPath, String outputPath);
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Button nextButton = findViewById(R.id.nextButton);
    nextButton.setOnClickListener(v -> {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
    });

    String filesDir = getFilesDir().getAbsolutePath();
    copyFile("git", filesDir + "/git");
    copyFile("ca-certificates.crt", filesDir + "/ca-certificates.crt");
    
  }
}

