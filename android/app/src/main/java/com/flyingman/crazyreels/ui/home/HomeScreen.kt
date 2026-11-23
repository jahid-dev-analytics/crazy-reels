package com.flyingman.crazyreels.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flyingman.crazyreels.BuildConfig
import com.flyingman.crazyreels.R
import com.flyingman.crazyreels.core.AppSettings
import com.flyingman.crazyreels.core.BrainState
import com.flyingman.crazyreels.counting.ReelApp
import com.flyingman.crazyreels.counting.ReelCounterService
import com.flyingman.crazyreels.counting.ReelStore
import com.flyingman.crazyreels.debug.LayoutRecorder
import com.flyingman.crazyreels.ui.brain.Brain

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val today by ReelStore.today.collectAsStateWithLifecycle()
    var counterOn by remember { mutableStateOf(ReelCounterService.isEnabled(context)) }
    var previewOn by rememberSaveable { mutableStateOf(false) }
    var previewReels by rememberSaveable { mutableFloatStateOf(0f) }

    LifecycleResumeEffect(Unit) {
        counterOn = ReelCounterService.isEnabled(context)
        ReelStore.refresh()
        onPauseOrDispose { }
    }

    val reels = if (previewOn) previewReels.toInt() else today.total
    val state = BrainState.forReels(reels)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!counterOn) {
                TurnOnCard {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            Brain(reels, Modifier.fillMaxWidth(0.75f).aspectRatio(1f))
            Text(reels.toString(), fontSize = 64.sp, fontWeight = FontWeight.Black)
            Text(
                stringResource(R.string.reels_today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(state.label), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(state.line),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PerAppCounts(today.perApp)
            if (counterOn) {
                val liveCounter by AppSettings.liveCounter.collectAsStateWithLifecycle()
                ToggleRow(stringResource(R.string.live_counter), liveCounter, AppSettings::setLiveCounter)
            }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                DeveloperSection(
                    previewOn = previewOn,
                    onPreviewChange = { previewOn = it },
                    previewReels = previewReels,
                    onPreviewReelsChange = { previewReels = it },
                )
            }
        }
    }
}

@Composable
private fun TurnOnCard(onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.turn_on_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.turn_on_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.turn_on_button))
            }
        }
    }
}

@Composable
private fun PerAppCounts(perApp: Map<ReelApp, Int>) {
    if (perApp.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        perApp.entries.sortedByDescending { it.value }.forEach { (app, count) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(app.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(count.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DeveloperSection(
    previewOn: Boolean,
    onPreviewChange: (Boolean) -> Unit,
    previewReels: Float,
    onPreviewReelsChange: (Float) -> Unit,
) {
    val recording by LayoutRecorder.enabled.collectAsStateWithLifecycle()
    val lines by LayoutRecorder.lines.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dev_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        ToggleRow(stringResource(R.string.dev_record), recording, LayoutRecorder::setEnabled)
        Text(
            stringResource(R.string.dev_record_lines, lines),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToggleRow(stringResource(R.string.dev_preview), previewOn, onPreviewChange)
        if (previewOn) {
            Slider(value = previewReels, onValueChange = onPreviewReelsChange, valueRange = 0f..250f)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
