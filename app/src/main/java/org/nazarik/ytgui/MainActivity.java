package org.nazarik.ytgui;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
public class MainActivity extends AppCompatActivity {
  static {
    try {
      System.loadLibrary("ytgui");
      Log.d("ytgui", "Loaded native library");
    } catch (UnsatisfiedLinkError e) {
      Log.e("ytgui", "Failed to load native library", e);
    }
  }
  public native void copyFile(AssetManager assetManager, String assetPath, String outputPath);
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    try {
      String filesDir = getFilesDir().getAbsolutePath();
      AssetManager assets = getAssets();
      if (assets == null) {
        Log.e("ytgui", "AssetManager is null");
        return;
      }
      copyFile(assets, "git", filesDir + "/git");
      copyFile(assets, "ca-certificates.crt", filesDir + "/ca-certificates.crt");
      Log.d("ytgui", "Assets copied successfully");
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy assets", e);
    }
    Button nextButton = findViewById(R.id.nextButton);
    nextButton.setOnClickListener(v -> {
      Intent intent = new Intent(this, DownloadActivity.class);
      startActivity(intent);
    });
  }
}

