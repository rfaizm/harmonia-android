package com.rfaizm.harmoniamusic.ui

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.rfaizm.harmoniamusic.data.Song

private const val WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE

/** Deleting media changed shape twice inside minSdk 24 to targetSdk 37; this names the three flows. */
internal enum class DeletePath {
    /** Android 11+: the system asks the user and does the delete itself. */
    SystemDialog,

    /** Android 10: the first attempt is refused with a consent request to launch, then it works. */
    RecoverableConsent,

    /** Android 7 to 9: the app deletes it, once it holds the storage permission. */
    StoragePermission,
}

internal fun deletePathFor(sdkInt: Int) = when {
    sdkInt >= 30 -> DeletePath.SystemDialog
    sdkInt == 29 -> DeletePath.RecoverableConsent
    else -> DeletePath.StoragePermission
}

/**
 * Returns a function that deletes a song's file, asking for whatever the running Android version needs first
 * (PRD phase 7). [onDeleted] runs only once the file is really gone, so the caller can drop the song from the
 * library and the queue. Nothing here can throw: a refusal or a missing file ends in a message, not a crash.
 */
@Composable
fun rememberSongDeleter(onDeleted: (Song) -> Unit): (Song) -> Unit {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    // The song waiting on a system dialog or a permission prompt.
    var pending by remember { mutableStateOf<Song?>(null) }
    var explainStorage by remember { mutableStateOf(false) }
    // "Don't ask again" was chosen, so prompting again does nothing and only settings can grant it.
    var blocked by remember { mutableStateOf(false) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    fun failed(song: Song) = toast("Couldn't delete “${song.displayTitle}”")

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val song = pending ?: return@rememberLauncherForActivityResult
        pending = null
        when {
            result.resultCode != Activity.RESULT_OK -> toast("Not deleted")
            // On Android 11+ the system already deleted it; on 10 the consent just unlocks a second attempt.
            Build.VERSION.SDK_INT >= 30 || deleteNow(context, song) -> onDeleted(song)
            else -> failed(song)
        }
    }

    val askStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val song = pending ?: return@rememberLauncherForActivityResult
        if (granted) {
            pending = null
            if (deleteNow(context, song)) onDeleted(song) else failed(song)
        } else {
            // Denied for good: only the settings screen can turn it back on, so say that instead of re-prompting.
            blocked = activity?.shouldShowRequestPermissionRationale(WRITE_STORAGE) == false
            if (blocked) explainStorage = true else { pending = null; toast("Storage access is needed to delete files") }
        }
    }

    if (explainStorage) {
        val song = pending
        AlertDialog(
            onDismissRequest = { explainStorage = false; pending = null },
            title = { Text("Storage access needed") },
            text = { Text("Android 9 and older need storage access before Harmonia can delete “${song?.displayTitle}”.") },
            confirmButton = {
                TextButton({
                    explainStorage = false
                    if (blocked) { openAppSettings(context); pending = null } else askStorage.launch(WRITE_STORAGE)
                }) { Text(if (blocked) "Open settings" else "Continue") }
            },
            dismissButton = { TextButton({ explainStorage = false; pending = null }) { Text("Cancel") } },
        )
    }

    fun delete(song: Song) {
        pending = song
        when (deletePathFor(Build.VERSION.SDK_INT)) {
            DeletePath.SystemDialog -> {
                val request = deleteRequest(context, song)
                if (request == null) { pending = null; failed(song) } else consent.launch(IntentSenderRequest.Builder(request).build())
            }
            DeletePath.RecoverableConsent -> runCatching { deleteOrConsent(context, song) }
                .onSuccess { needed ->
                    if (needed == null) { pending = null; onDeleted(song) } else consent.launch(IntentSenderRequest.Builder(needed).build())
                }
                .onFailure { pending = null; failed(song) }
            DeletePath.StoragePermission -> when {
                ContextCompat.checkSelfPermission(context, WRITE_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    pending = null
                    if (deleteNow(context, song)) onDeleted(song) else failed(song)
                }
                // Explain before asking a second time, then let the system prompt do the asking.
                activity?.shouldShowRequestPermissionRationale(WRITE_STORAGE) == true -> explainStorage = true
                else -> askStorage.launch(WRITE_STORAGE)
            }
        }
    }
    return ::delete
}

private fun uriOf(song: Song): Uri =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id.toLong())

/** True once the song is gone; a row that was already deleted elsewhere counts as gone. */
private fun deleteNow(context: Context, song: Song): Boolean =
    runCatching { context.contentResolver.delete(uriOf(song), null, null) }.isSuccess

/** Android 11+: the intent that makes the system show its own delete confirmation, or null if it can't be built. */
private fun deleteRequest(context: Context, song: Song): IntentSender? {
    if (Build.VERSION.SDK_INT < 30) return null
    return runCatching { MediaStore.createDeleteRequest(context.contentResolver, listOf(uriOf(song))).intentSender }.getOrNull()
}

/** Android 10: null when the song is already gone, otherwise the consent the user has to grant first. */
private fun deleteOrConsent(context: Context, song: Song): IntentSender? {
    if (Build.VERSION.SDK_INT < 29) return null
    return try {
        context.contentResolver.delete(uriOf(song), null, null)
        null
    } catch (e: RecoverableSecurityException) {
        e.userAction.actionIntent.intentSender
    } // any other SecurityException is a real failure and is reported by the caller
}

private fun openAppSettings(context: Context) = context.startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
)
