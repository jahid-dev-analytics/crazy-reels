package com.flyingman.crazyreels.counting

/** Apps whose short videos we count. The service only listens to these packages. */
enum class ReelApp(val packageName: String, val displayName: String) {
    INSTAGRAM("com.instagram.android", "Instagram"),
    FACEBOOK("com.facebook.katana", "Facebook"),
    FACEBOOK_LITE("com.facebook.lite", "Facebook Lite"),
    TIKTOK("com.zhiliaoapp.musically", "TikTok"),
    YOUTUBE("com.google.android.youtube", "YouTube"),
    SNAPCHAT("com.snapchat.android", "Snapchat");

    companion object {
        private val byPackage = entries.associateBy { it.packageName }
        fun fromPackage(pkg: CharSequence?): ReelApp? = pkg?.let { byPackage[it.toString()] }
    }
}
