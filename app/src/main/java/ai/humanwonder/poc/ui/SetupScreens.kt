package ai.humanwonder.poc.ui

import ai.humanwonder.poc.DeviceCheck
import ai.humanwonder.poc.model.ModelSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun BrandHeader(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("✦", color = HW.Primary, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text("Human Wonder", color = HW.OnSurface, fontFamily = HW.Headline, fontSize = 22.sp)
    }
}

@Composable
fun MonoLabel(text: String, color: Color = HW.OnSurfaceVariant, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(Locale.ROOT),
        modifier = modifier,
        color = color,
        fontFamily = HW.Mono,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = HW.PrimaryContainer,
            contentColor = HW.OnPrimaryContainer,
            disabledContainerColor = HW.SurfaceHigh,
            disabledContentColor = HW.Outline,
        ),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * The permission step. Nothing is downloaded until the user explicitly agrees here.
 */
@Composable
fun ConsentScreen(check: DeviceCheck, onAllow: () -> Unit, onImport: () -> Unit, onRecheck: () -> Unit) {
    val resuming = check.alreadyDownloaded > 0
    val remaining = ModelSpec.SIZE_BYTES - check.alreadyDownloaded
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        BrandHeader()
        Spacer(Modifier.height(56.dp))
        Text(
            "Ask the sky.",
            color = HW.OnSurface,
            fontFamily = HW.Headline,
            fontSize = 40.sp,
            lineHeight = 46.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Before the first question, Human Wonder needs its voice: a small language model " +
                "that lives on this phone and answers without the internet.",
            color = HW.OnSurfaceVariant,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(28.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HW.Surface.copy(alpha = 0.85f))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow("Model", "${ModelSpec.DISPLAY_NAME} · ${ModelSpec.QUANT} (${ModelSpec.LICENSE})")
            InfoRow(
                "Download",
                if (resuming) "${mb(remaining)} left of ${mb(ModelSpec.SIZE_BYTES)}, once"
                else "${mb(ModelSpec.SIZE_BYTES)}, once",
            )
            InfoRow("After that", "Works fully offline. Your questions never leave this phone.")
            InfoRow("Source", ModelSpec.SOURCE)
        }

        Spacer(Modifier.height(16.dp))
        if (!check.online) {
            Notice("You're offline. Connect to Wi-Fi to download.", HW.Gold)
            TextButton(onClick = onRecheck) { Text("Check again", color = HW.Primary) }
        } else if (check.onMeteredNetwork) {
            Notice("You're on mobile data. Wi-Fi is recommended for a ${mb(remaining)} download.", HW.Gold)
        }
        if (!check.enoughStorage) {
            Notice(
                "Not enough free space: ${mb(check.bytesNeeded)} needed, ${mb(check.freeStorageBytes)} free.",
                HW.Error,
            )
        }
        if (check.lowRam) {
            Notice(
                "This phone has ${gb(check.totalRamBytes)} of RAM. It should work, but answers may be slow.",
                HW.OnSurfaceVariant,
            )
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            text = if (resuming) "Resume download · ${mb(remaining)}" else "Allow download · ${mb(ModelSpec.SIZE_BYTES)}",
            enabled = check.enoughStorage && check.online,
            onClick = onAllow,
        )
        TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("I already have the model file", color = HW.OnSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        MonoLabel("On-device · private · keep looking up", modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        MonoLabel(label, color = HW.Outline)
        Spacer(Modifier.height(2.dp))
        Text(value, color = HW.OnSurface, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun Notice(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

/** Download / verify / import progress. [total] < 0 means unknown size. */
@Composable
fun ProgressScreen(
    title: String,
    subtitle: String,
    done: Long,
    total: Long,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        BrandHeader()
        Spacer(Modifier.weight(1f))
        Text(title, color = HW.OnSurface, fontFamily = HW.Headline, fontSize = 30.sp, lineHeight = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text(subtitle, color = HW.OnSurfaceVariant, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))
        if (total > 0) {
            LinearProgressIndicator(
                progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = HW.PrimaryContainer,
                trackColor = HW.SurfaceHigh,
            )
            Spacer(Modifier.height(10.dp))
            MonoLabel("${mb(done)} / ${mb(total)} · ${(done * 100 / total)}%")
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = HW.PrimaryContainer,
                trackColor = HW.SurfaceHigh,
            )
            if (done > 0) {
                Spacer(Modifier.height(10.dp))
                MonoLabel(mb(done))
            }
        }
        Spacer(Modifier.weight(1f))
        if (actionLabel != null) {
            TextButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel, color = HW.OnSurfaceVariant)
            }
        }
    }
}

@Composable
fun LoadingScreen(modelLabel: String) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = HW.PrimaryContainer, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(20.dp))
        Text("Waking the model…", color = HW.OnSurface, fontFamily = HW.Headline, fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        MonoLabel(modelLabel)
    }
}

@Composable
fun FailedScreen(
    message: String,
    offerRedownload: Boolean,
    onRetry: () -> Unit,
    onDeleteAndRedownload: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        BrandHeader()
        Spacer(Modifier.weight(1f))
        Text("The sky went quiet.", color = HW.OnSurface, fontFamily = HW.Headline, fontSize = 30.sp)
        Spacer(Modifier.height(12.dp))
        Text(message, color = HW.OnSurfaceVariant, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Start)
        Spacer(Modifier.weight(1f))
        PrimaryButton("Try again", onClick = onRetry)
        if (offerRedownload) {
            TextButton(onClick = onDeleteAndRedownload, modifier = Modifier.fillMaxWidth()) {
                Text("Delete model and start over", color = HW.OnSurfaceVariant)
            }
        }
        TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import a model file instead", color = HW.OnSurfaceVariant)
        }
    }
}

internal fun mb(bytes: Long): String = "%d MB".format(Locale.ROOT, (bytes + 500_000) / 1_000_000)

internal fun gb(bytes: Long): String = "%.1f GB".format(Locale.ROOT, bytes / 1_073_741_824.0)
