package com.phonons.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.phonons.app.R
import com.phonons.app.data.CrashLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Eigentone's own log, read live, for working out what went wrong. Android lets an app read only what its own
 * process wrote, so this never shows anything from other apps. Lines wrap rather than scroll sideways.
 */
@Composable
fun DebugConsoleScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var live by remember { mutableStateOf(true) }
    var everything by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val list = rememberLazyListState()

    BackHandler(onBack = onClose)

    LaunchedEffect(live, everything) {
        do {
            val read = withContext(Dispatchers.IO) { readOwnLog(everything) }
            val atEnd = !list.canScrollForward
            if (read != lines) {
                lines = read
                // Follows new lines only while already at the bottom, so reading further up isn't interrupted.
                if (atEnd && read.isNotEmpty()) list.scrollToItem(read.lastIndex)
            }
            if (live) delay(REFRESH_MS)
        } while (live && isActive)
    }

    fun asText() = (listOf(CrashLog.deviceSummary(context), "") + lines).joinToString("\n")

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Bar)
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = 56.dp)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_back), tint = Palette.Text)
            }
            Text(stringResource(R.string.ui_debug_console), style = MaterialTheme.typography.titleLarge, color = Palette.Text, modifier = Modifier.weight(1f))
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SlidingToggle(
                options = listOf(
                    R.drawable.ic_phonons to stringResource(R.string.ui_console_eigentone),
                    R.drawable.ic_spectrum to stringResource(R.string.ui_console_everything),
                ),
                selected = if (everything) 1 else 0,
                onSelect = { everything = it == 1 },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.ui_console_note),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Palette.Surface, PanelShape),
        ) {
            if (lines.isEmpty()) {
                Text(
                    stringResource(R.string.ui_console_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextDim,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                    itemsIndexed(lines) { _, line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = when (levelOf(line)) {
                                'E', 'F' -> Palette.Danger
                                'W' -> Palette.Lit
                                else -> Palette.TextDim
                            },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Bar)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = { live = !live }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(if (live) R.string.ui_console_pause else R.string.ui_console_resume), maxLines = 1)
            }
            FilledTonalButton(
                onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Eigentone log", asText()))
                    copied = true
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(if (copied) R.string.ui_copied else R.string.ui_console_copy), maxLines = 1) }
            FilledTonalButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Eigentone log")
                        .putExtra(Intent.EXTRA_TEXT, asText())
                    context.startActivity(Intent.createChooser(send, context.getString(R.string.ui_console_share)))
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ui_console_share), maxLines = 1) }
        }
    }
}

/** The latest lines this process logged, either Eigentone's own messages or everything, including Android's. */
private fun readOwnLog(everything: Boolean): List<String> = runCatching {
    val process = ProcessBuilder(
        "logcat", "-d", "-v", "time", "-t", "$MAX_LINES", "--pid=${android.os.Process.myPid()}",
    ).redirectErrorStream(true).start()
    val all = process.inputStream.bufferedReader().use { it.readLines() }
    process.waitFor()
    all.filter { it.isNotBlank() && !it.startsWith("--------- beginning of") }
        .filter { everything || it.contains(" Phonons") || it.contains("/Phonons") || it.contains("AndroidRuntime") }
}.getOrElse { listOf("E/Console: Could not read the log: ${it.message}") }

/** The level letter in a "time" format line, such as the W in "11-05 09:00:01.234 W/Tag( 123): text". */
private fun levelOf(line: String): Char? {
    val slash = line.indexOf('/')
    return if (slash > 0) line[slash - 1] else null
}

private const val MAX_LINES = 800
private const val REFRESH_MS = 1500L
