package com.tejas.passvault.ui.hidden

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.ui.photos.PhotoVaultScreen

private enum class HiddenSection { HUB, PASSWORDS, PHOTOS, NOTES }

/**
 * The hidden vault's home screen: a small hub leading to its own secret passwords, photos and
 * notes. Reachable only by drawing the hidden vault's pattern on the login screen.
 */
@Composable
fun HiddenVaultScreen(vm: VaultViewModel, onLock: () -> Unit) {
    var section by remember { mutableStateOf(HiddenSection.HUB) }

    BackHandler(enabled = section != HiddenSection.HUB) { section = HiddenSection.HUB }

    when (section) {
        HiddenSection.HUB -> HiddenHub(onOpen = { section = it }, onLock = onLock)
        HiddenSection.PASSWORDS -> HiddenPasswordsScreen(vm = vm, onBack = { section = HiddenSection.HUB })
        HiddenSection.PHOTOS -> {
            val photos by vm.hiddenPhotos.collectAsState()
            PhotoVaultScreen(
                photos = photos,
                onAddPhoto = { vm.addHiddenPhoto(it) },
                onDeletePhoto = { vm.deleteHiddenPhoto(it) },
                loadPhotoBytes = { vm.loadHiddenPhotoBytes(it) },
                onBack = { section = HiddenSection.HUB },
                title = "Secret photos"
            )
        }
        HiddenSection.NOTES -> HiddenNotesScreen(vm = vm, onBack = { section = HiddenSection.HUB })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenHub(onOpen: (HiddenSection) -> Unit, onLock: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private vault") },
                navigationIcon = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Secret passwords") },
                supportingContent = { Text("Passwords only you know about") },
                modifier = Modifier.fillMaxWidth().clickable { onOpen(HiddenSection.PASSWORDS) }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Secret photos") },
                supportingContent = { Text("Pictures from your gallery, kept private") },
                modifier = Modifier.fillMaxWidth().clickable { onOpen(HiddenSection.PHOTOS) }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Secret notes") },
                supportingContent = { Text("Codes, PINs, anything you want to jot down") },
                modifier = Modifier.fillMaxWidth().clickable { onOpen(HiddenSection.NOTES) }
            )
            HorizontalDivider()
        }
    }
}
