package com.tejas.passvault.ui.photos

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.data.VaultPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoVaultScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photos by vm.photos.collectAsState()
    var viewingPhoto by remember { mutableStateOf<VaultPhoto?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isAdding = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes != null) vm.addPhoto("", bytes)
            isAdding = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo vault") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isAdding) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (photos.isEmpty()) {
                Text(
                    "No photos yet. Tap the camera icon to add one from your gallery.",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoThumbnail(vm = vm, photo = photo, onClick = { viewingPhoto = photo })
                    }
                }
            }
        }
    }

    viewingPhoto?.let { photo ->
        PhotoViewerDialog(
            vm = vm,
            photo = photo,
            onDismiss = { viewingPhoto = null },
            onDelete = {
                scope.launch {
                    vm.deletePhoto(photo.id)
                    viewingPhoto = null
                }
            }
        )
    }
}

@Composable
private fun PhotoThumbnail(vm: VaultViewModel, photo: VaultPhoto, onClick: () -> Unit) {
    var bitmap by remember(photo.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo.id) {
        val bytes = vm.loadPhotoBytes(photo.id)
        bitmap = bytes?.let { decodeBitmap(it) }
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(onClick = onClick)
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun PhotoViewerDialog(vm: VaultViewModel, photo: VaultPhoto, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var bitmap by remember(photo.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo.id) {
        val bytes = vm.loadPhotoBytes(photo.id)
        bitmap = bytes?.let { decodeBitmap(it) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Close") }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun decodeBitmap(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
