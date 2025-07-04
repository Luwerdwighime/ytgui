-keep class org.nazarik.ytgui.** { ; }
-keep class org.eclipse.jgit.** { *; }
-keep class com.jcraft.** { *; }
-dontwarn androidx.*
-dontwarn org.eclipse.jgit.**

# yt-dlp — не трогать логику Python
-keep class * {
  public *;
}

