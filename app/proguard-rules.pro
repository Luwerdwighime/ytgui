-keep class org.nazarik.ytgui.** { ; }
-keep class org.eclipse.jgit.** { *; }
-dontwarn androidx.*
-dontwarn org.eclipse.jgit.**

# yt-dlp — не трогать логику Python
-keep class * {
  public *;
}

