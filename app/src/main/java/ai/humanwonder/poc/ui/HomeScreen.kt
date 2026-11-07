package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import ai.humanwonder.poc.SkyState
import ai.humanwonder.poc.data.local.entities.Conversation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/** The three questions under "Try asking". */
private val TRY_ASKING = listOf(
    "What does Earth look like tonight?",
    "Where is the Moon right now?",
    "How fast are you moving?",
)

/** "Good morning|afternoon|evening|night", from the phone's clock (as in the preview). */
fun greetingFor(time: LocalTime = LocalTime.now()): String {
    val h = time.hour
    return when {
        h < 5 -> "Good night"
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        h < 22 -> "Good evening"
        else -> "Good night"
    }
}

/** When a transmission last changed: "9:42 PM" today, "Yesterday", "Mon" this week, else "Mar 4". */
fun transmissionTime(epochMillis: Long, today: LocalDate = LocalDate.now()): String {
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val day = at.toLocalDate()
    return when {
        day == today -> at.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        day == today.minusDays(1) -> "Yesterday"
        day.isAfter(today.minusDays(7)) -> at.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
        else -> at.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}

/**
 * Home (matches the preview's Home artboard): plain black, a greeting, one photo card for
 * Orbit 412 (the Earth at night from orbit), the last three transmissions in a quiet grouped
 * list, and a few questions to try. No aurora, dot grid or glowing icons here; the chat keeps
 * the sky.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    name: String,
    sky: SkyState,
    recent: List<Conversation>,
    onMenu: () -> Unit,
    onProfile: () -> Unit,
    onTalk: () -> Unit,
    onOpen: (String) -> Unit,
    onSeeAll: () -> Unit,
    onAsk: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val displayName = name.trim().ifBlank { "Wanderer" }
    Box(Modifier.fillMaxSize().background(HW.Space)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Header: menu, lockup, avatar (64dp).
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HwIconButton(R.drawable.ic_menu, "Open menu", iconSize = 24.dp, onClick = onMenu)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { BrandLockup() }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onProfile)
                        .semantics { contentDescription = "Profile and settings" },
                    contentAlignment = Alignment.Center,
                ) { InitialAvatar(displayName, size = 36.dp) }
            }

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    "${greetingFor()}, $displayName",
                    color = Color.White,
                    style = HWType.HomeGreeting.capScale(),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                )

                OrbitCard(sky, onTalk, onRetry)

                if (recent.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 28.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Recent transmissions", color = Color.White, style = HWType.HubTitle, modifier = Modifier.weight(1f))
                        Box(
                            Modifier
                                .heightIn(min = 48.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onSeeAll)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("See all", color = HW.Primary, style = HWType.ChipStrong, maxLines = 1, softWrap = false)
                        }
                    }
                    val group = RoundedCornerShape(20.dp)
                    Column(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(group)
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), group),
                    ) {
                        recent.take(3).forEachIndexed { i, c ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(HW.Divider))
                            RecentRow(c, onClick = { onOpen(c.id) })
                        }
                    }
                }

                Text(
                    "Try asking",
                    color = Color.White,
                    style = HWType.HubTitle,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
                )
                FlowRow(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TRY_ASKING.forEach { q -> AskChip(q, onClick = { onAsk(q) }) }
                }
            }
        }
    }
}

/**
 * Places the Earth photo at full card width, 30% of the width above the card's top edge, so the
 * glowing horizon always sits in the clear band above the text (the preview's margin-top: -30%).
 */
private val EarthOnCard = Alignment { size, space, _ ->
    IntOffset((space.width - size.width) / 2, -(space.width * 0.30f).roundToInt())
}

/**
 * The Orbit 412 card: the Earth at night from orbit, then live status, name, one line and the
 * teal call to action on a dark band that grows with the text (so text never sits on the bright
 * horizon, even at 150% font size).
 */
@Composable
private fun OrbitCard(sky: SkyState, onTalk: () -> Unit, onRetry: () -> Unit) {
    val (status, dot) = when (sky) {
        is SkyState.Aligning -> "Rising over the horizon, ${(sky.progress * 100).toInt()}%" to HW.Gold
        SkyState.Waking -> "Finding your signal…" to HW.Gold
        SkyState.Ready -> "Overhead now, 412 km up" to HW.Primary
        is SkyState.Failed -> "Signal lost" to HW.Error
    }
    val failed = sky as? SkyState.Failed
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(HW.HeroCard)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_earth_orbit),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = EarthOnCard,
            modifier = Modifier.matchParentSize(),
        )
        Column(Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(120.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val fade = (56.dp.toPx() / size.height).coerceIn(0f, 1f)
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                fade to Color.Black.copy(alpha = 0.8f),
                                1f to Color.Black.copy(alpha = 0.92f),
                            ),
                        )
                    }
                    .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (failed == null) PulsingDot(dot, size = 6.dp, periodMs = 1600)
                    else Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(8.dp))
                    Text(status, color = HW.IconMuted, style = HWType.Subline)
                }
                Text(
                    "Orbit 412",
                    color = Color.White,
                    style = HWType.HeroTitle,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    failed?.message ?: "Your Human Wonder satellite. Ask it what it sees tonight.",
                    color = HW.IconMuted,
                    style = HWType.ChatSub,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                if (failed != null) {
                    PrimaryButton("Try again", onClick = onRetry)
                } else {
                    PrimaryButton("Talk to Orbit 412", onClick = onTalk)
                }
            }
        }
    }
}

@Composable
private fun RecentRow(c: Conversation, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(c.title, color = HW.Ink, style = HWType.RowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(transmissionTime(c.updatedAt), color = HW.InkTertiary, style = HWType.Subline, maxLines = 1)
        }
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            HwIcon(R.drawable.ic_chevron_right, HW.Chevron, 20.dp)
        }
    }
}

/** A question chip: 14/20 text in a white 14% outline, wrapping onto a new line when needed. */
@Composable
private fun AskChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, color = HW.Chip, style = HWType.Chip)
    }
}
