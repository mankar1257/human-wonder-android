package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import ai.humanwonder.poc.ai.Frame
import ai.humanwonder.poc.ai.FrameCatalog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.LruCache
import android.widget.ImageView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A frame from Orbit's archive under an answer: an astronaut photograph of Earth, or a short loop
 * from the station's time-lapses. It fades in once the words have arrived, says where it is from,
 * and opens full screen (with NASA's credit) when tapped.
 */
@Composable
fun FrameCard(frame: Frame, modifier: Modifier = Modifier) {
    var open by rememberSaveable(frame.id) { mutableStateOf(false) }
    var shown by remember(frame.id) { mutableStateOf(false) }
    var failed by remember(frame.id) { mutableStateOf(false) }
    // A picture that cannot be read leaves no gap: the answer simply has no frame.
    if (failed) return
    val appear by animateFloatAsState(if (shown) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "frame")
    val described = "${frame.caption}, a frame from Orbit's archive" + if (frame.isLoop) ", time-lapse" else ""

    Column(modifier.fillMaxWidth().padding(top = 14.dp)) {
        Box(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .aspectRatio(FRAME_ASPECT)
                .graphicsLayer {
                    alpha = appear
                    val s = 0.97f + 0.03f * appear
                    scaleX = s
                    scaleY = s
                }
                .clip(FRAME_SHAPE)
                .background(HW.Bubble)
                .border(1.dp, HW.GlassBorder, FRAME_SHAPE)
                .clickable(role = Role.Button, onClickLabel = "Open full screen") { open = true }
                .semantics { contentDescription = described },
        ) {
            FramePicture(frame, ContentScale.Crop, Modifier.fillMaxSize(), onReady = { shown = true }, onFailed = { failed = true })
            if (frame.isLoop) {
                Text(
                    "Time-lapse",
                    color = HW.Ink,
                    style = HWType.ChatMeta,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Row(Modifier.padding(top = 8.dp).graphicsLayer { alpha = appear }, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "From Orbit’s archive · ${frame.caption}",
                color = HW.InkSecondary,
                style = HWType.ChatMeta,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (open) FrameViewer(frame, onClose = { open = false })
}

/** The picture full screen: whole, on black, with its caption and NASA's credit. */
@Composable
private fun FrameViewer(frame: Frame, onClose: () -> Unit) {
    val context = LocalContext.current
    val credit = remember { FrameCatalog.load(context).credit }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // Dialogs start a new composition, so the app's text and display size limits are given again.
        HumanWonderScale {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClickLabel = "Close", onClick = onClose)) {
                FramePicture(frame, ContentScale.Fit, Modifier.fillMaxSize().semantics { contentDescription = frame.caption })
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        HwIconButton(R.drawable.ic_close, "Close", tint = HW.Ink, onClick = onClose)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Text(frame.caption, color = HW.Ink, style = HWType.ChatTitle)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "From Orbit’s archive. " + (if (frame.isLoop) "Time-lapse from frames ${frame.source}. " else "Frame ${frame.source}. ") + credit + ".",
                            color = HW.InkSecondary,
                            style = HWType.ChatMeta,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws the picture: photos as a bitmap decoded to screen size off the main thread, loops as an
 * animated drawable that plays while it is on screen (still, on its first frame, when the phone's
 * animations are turned off).
 */
@Composable
private fun FramePicture(
    frame: Frame,
    scale: ContentScale,
    modifier: Modifier,
    onReady: () -> Unit = {},
    onFailed: () -> Unit = {},
) {
    val context = LocalContext.current
    if (frame.isLoop) {
        val drawable by produceState<Drawable?>(null, frame.id) {
            value = withContext(Dispatchers.IO) { runCatching { decodeLoop(context, frame) }.getOrNull() }
            if (value == null) onFailed()
        }
        val d = drawable ?: return
        val still = remember { animationsOff(context) }
        AndroidView(
            factory = { ImageView(it).apply { this.scaleType = if (scale == ContentScale.Crop) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER } },
            update = { it.setImageDrawable(d) },
            modifier = modifier,
        )
        DisposableEffect(d) {
            if (d is AnimatedImageDrawable && !still) d.start()
            onReady()
            onDispose { if (d is AnimatedImageDrawable) d.stop() }
        }
    } else {
        val bitmap by produceState<ImageBitmap?>(null, frame.id) {
            value = withContext(Dispatchers.IO) { runCatching { decodePhoto(context, frame) }.getOrNull() }
            if (value == null) onFailed()
        }
        val b = bitmap ?: return
        Image(b, contentDescription = null, contentScale = scale, modifier = modifier)
        DisposableEffect(b) {
            onReady()
            onDispose { }
        }
    }
}

private val FRAME_SHAPE = RoundedCornerShape(20.dp)
/** Every frame is cropped to 3:2 in the chat (the archive's photos are 3:2); full screen shows it whole. */
private const val FRAME_ASPECT = 3f / 2f

/** Decoded photos, so scrolling back up does not decode them again. */
private val photoCache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

private fun decodePhoto(context: Context, frame: Frame): ImageBitmap {
    photoCache.get(frame.id)?.let { return it }
    val target = context.resources.displayMetrics.widthPixels.coerceAtLeast(720)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(frame.assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= target) sample *= 2
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bmp = context.assets.open(frame.assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
        ?: error("Could not decode ${frame.assetPath}")
    return bmp.asImageBitmap().also { photoCache.put(frame.id, it) }
}

private fun decodeLoop(context: Context, frame: Frame): Drawable =
    ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.assets, frame.assetPath)).also {
        if (it is AnimatedImageDrawable) it.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
    }

private fun animationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
