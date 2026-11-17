package com.sonara.app.feature.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sonara.app.core.design.components.SonaraPlaceholderBody
import com.sonara.app.core.design.components.SonaraTopBar

@Composable
fun FoldersScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        SonaraTopBar(title = "FOLDERS")
        SonaraPlaceholderBody(
            milestone = "M3",
            summary = "Folders will show indexed audio folders and folders you explicitly grant access to.",
        )
    }
}
