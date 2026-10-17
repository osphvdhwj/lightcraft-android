package ai.storyteller.lightcraft.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp

/** The library grid, or the editor when a photo is open; snackbars for both. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LightcraftRoot(vm: AppViewModel, onPickPhotos: () -> Unit, onPickFiles: () -> Unit, onCamera: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    // kept here so the grid's scroll position survives a visit to the editor
    val grid = rememberLazyListState()
    LaunchedEffect(vm) {
        for (m in vm.messages) {
            val result = snackbar.showSnackbar(
                m.text,
                actionLabel = m.actionLabel,
                withDismissAction = m.actionLabel == null,
                duration = if (m.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) m.action?.invoke()
        }
    }
    val editor = vm.editor
    // the library's message (and its Undo) stays with the library
    LaunchedEffect(editor != null) { if (editor != null) snackbar.currentSnackbarData?.dismiss() }
    // the editor fading out still needs the photo it showed
    val last = remember { arrayOfNulls<EditorState>(1) }
    if (editor != null) last[0] = editor
    BackHandler(enabled = editor != null) { vm.back() }
    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = editor != null, label = "screen") { editing ->
            val ed = if (editing) vm.editor ?: last[0] else null
            if (ed == null) LibraryScreen(vm, snackbar, grid, onPickPhotos, onPickFiles, onCamera) else EditorScreen(vm, ed, snackbar)
        }
        // M3E quick-action FAB — library only
        if (editor == null) {
            QuickActionsFab(
                onPickPhotos = onPickPhotos,
                onPickFiles = onPickFiles,
                onCamera = onCamera,
            )
        }
        if (vm.slideshow != null) Slideshow(vm)
    }
}

/**
 * Material 3 Expressive quick-action menu for the library.
 * Three "add" entry points, springy expansion, back closes the menu before exiting.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoxScope.QuickActionsFab(
    onPickPhotos: () -> Unit,
    onPickFiles: () -> Unit,
    onCamera: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    BackHandler(enabled = open) { open = false }

    FloatingActionButtonMenu(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp),
        expanded = open,
        button = {
            ToggleFloatingActionButton(
                checked = open,
                onCheckedChange = {
                    haptics.performHapticFeedback(
                        if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                    )
                    open = it
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add photos")
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); open = false; onPickPhotos() },
            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
            text = { Text("Import photos") },
        )
        FloatingActionButtonMenuItem(
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); open = false; onPickFiles() },
            icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
            text = { Text("Import files") },
        )
        FloatingActionButtonMenuItem(
            onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); open = false; onCamera() },
            icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
            text = { Text("Take photo") },
        )
    }
}
