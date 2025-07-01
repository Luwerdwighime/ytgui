package org.nazarik.ytgui;
import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class GitUtils {
  // Копирование файла из assets в files
  public static void copyFile(Context context, String assetPath, String outputPath) throws Exception {
    try (InputStream in = context.getAssets().open(assetPath)) {
      File outFile = new File(outputPath);
      File parentDir = outFile.getParentFile();
      if (!parentDir.exists() && !parentDir.mkdirs()) {
        Log.e("ytgui", "Failed to create directory: " + parentDir.getAbsolutePath());
        throw new Exception("Failed to create directory");
      }
      if (!parentDir.canWrite()) {
        Log.e("ytgui", "Cannot write to directory: " + parentDir.getAbsolutePath());
        throw new Exception("Cannot write to directory");
      }
      try (FileOutputStream out = new FileOutputStream(outFile)) {
        byte[] buffer = new byte[1024];
        int bytes;
        while ((bytes = in.read(buffer)) != -1) {
          out.write(buffer, 0, bytes);
        }
      }
    } catch (Exception e) {
      Log.e("ytgui", "Failed to copy " + assetPath + ": " + e.getMessage());
      throw e;
    }
  }

  // Копирование папки из assets в files с рекурсией
  public static void copyFolder(Context context, String assetDir, String outputDir) throws Exception {
    String[] files = context.getAssets().list(assetDir);
    if (files == null) return;
    File outDir = new File(outputDir);
    if (!outDir.exists() && !outDir.mkdirs()) {
      Log.e("ytgui", "Failed to create directory: " + outputDir);
      throw new Exception("Failed to create directory");
    }
    for (String file : files) {
      String assetPath = assetDir + "/" + file;
      String outPath = outputDir + "/" + file;
      String[] subFiles = context.getAssets().list(assetPath);
      if (subFiles != null && subFiles.length > 0) {
        copyFolder(context, assetPath, outPath);
      } else {
        copyFile(context, assetPath, outPath);
      }
    }
  }
}

