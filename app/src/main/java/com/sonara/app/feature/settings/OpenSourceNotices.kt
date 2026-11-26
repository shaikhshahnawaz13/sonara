package com.sonara.app.feature.settings

/** One open-source component SONARA is built with. No version numbers: they would go stale. */
internal data class OpenSourceNotice(
    val name: String,
    val author: String,
    val licence: String,
    val usedFor: String,
)

internal object OpenSourceNotices {

    const val APACHE_2 = "Apache License 2.0"
    const val APACHE_2_URL = "apache.org/licenses/LICENSE-2.0"
    const val OFL_1_1 = "SIL Open Font License 1.1"

    val all: List<OpenSourceNotice> = listOf(
        OpenSourceNotice("Jetpack Compose and Material 3", "Google / Android Open Source Project", APACHE_2, "Every screen and component"),
        OpenSourceNotice("AndroidX Activity, Core, Lifecycle and Navigation", "Google / Android Open Source Project", APACHE_2, "App structure and navigation"),
        OpenSourceNotice("AndroidX Room", "Google / Android Open Source Project", APACHE_2, "Library, playlists and history storage"),
        OpenSourceNotice("AndroidX DataStore", "Google / Android Open Source Project", APACHE_2, "Saving your settings"),
        OpenSourceNotice("AndroidX Media3 (ExoPlayer and MediaSession)", "Google / Android Open Source Project", APACHE_2, "Audio playback and the media notification"),
        OpenSourceNotice("Guava", "Google", APACHE_2, "Support code used by Media3"),
        OpenSourceNotice("Kotlin and kotlinx.coroutines", "JetBrains", APACHE_2, "The language and background work"),
        OpenSourceNotice("Barlow Condensed (font)", "The Barlow Project Authors", OFL_1_1, "Headings, kickers and buttons"),
    )

    const val ACKNOWLEDGEMENT =
        "SONARA plays only the music already on your device. It has no accounts, no ads and no online services, " +
            "and it is made possible by the open-source projects above. Thank you to everyone who builds and maintains them."

    const val LICENCE_NOTE =
        "Unless a line says otherwise, each project above is licensed under the Apache License, Version 2.0. You may obtain " +
            "a copy of the licence at " + APACHE_2_URL + ". The Barlow Condensed font is licensed under the SIL Open Font " +
            "License 1.1 (scripts.sil.org/OFL). The software is provided \"as is\", without warranties or conditions of any kind."
}
