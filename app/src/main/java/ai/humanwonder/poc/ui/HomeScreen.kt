package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import ai.humanwonder.poc.SkyState
import ai.humanwonder.poc.data.local.entities.Conversation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The three questions under "Ask tonight". */
private val ASK_TONIGHT = listOf(
    "What does Earth look like tonight?",
    "Where is the Moon right now?",
    "Tell me about the Milky Way",
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
 * Home: the aurora a little lower than the empty chat (calm 0.35) under a faint dot grid, a
 * greeting, the Orbit 412 card, the last three transmissions and a few questions for tonight.
 */
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
        GalaxyBackground(Modifier.fillMaxSize(), energy = 0f, calm = 0.35f)
        DotGrid(Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.4f to Color.Transparent),
            ),
        )

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
                // Greeting.
                Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
                    Text(
                        "${greetingFor()}, $displayName",
                        color = Color.White,
                        style = HWType.WelcomeTitle.capScale(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(homeSubline(sky), color = HW.InkSecondary, style = HWType.ChatSub)
                }

                OrbitCard(sky, onTalk, onRetry)

                if (recent.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 28.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Recent transmissions",
                            color = Color.White,
                            style = HWType.HubTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .heightIn(min = 48.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onSeeAll)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("See all", color = HW.Primary, style = HWType.ChipStrong, maxLines = 1, softWrap = false)
                        }
                    }
                    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().glass()) {
                        recent.take(3).forEachIndexed { i, c ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(HW.Divider))
                            RecentRow(c, onClick = { onOpen(c.id) })
                        }
                    }
                }

                Text(
                    "Ask tonight",
                    color = Color.White,
                    style = HWType.HubTitle,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ASK_TONIGHT.forEach { q -> AskChip(q, onClick = { onAsk(q) }) }
                }
            }
        }
    }
}

private fun homeSubline(sky: SkyState): String = when (sky) {
    is SkyState.Aligning -> "Orbit 412 is rising over the horizon, ${(sky.progress * 100).toInt()}%"
    SkyState.Waking -> "Orbit 412 is finding your signal…"
    SkyState.Ready -> "Orbit 412 is overhead and listening."
    is SkyState.Failed -> "Orbit 412’s signal is lost."
}

/** The glass Orbit 412 card: mark, live status, one line about it and the teal call to action. */
@Composable
private fun OrbitCard(sky: SkyState, onTalk: () -> Unit, onRetry: () -> Unit) {
    val (status, dot) = when (sky) {
        is SkyState.Aligning -> "Rising over the horizon, ${(sky.progress * 100).toInt()}%" to HW.Gold
        SkyState.Waking -> "Finding your signal…" to HW.Gold
        SkyState.Ready -> "Overhead now, 412 km up" to HW.Primary
        is SkyState.Failed -> "Signal lost" to HW.Error
    }
    val failed = sky as? SkyState.Failed
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
            .fillMaxWidth()
            .glass()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrbitMark(32.dp, active = sky !is SkyState.Ready && failed == null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Orbit 412", color = Color.White, style = HWType.HubTitle)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (failed == null) PulsingDot(dot, size = 6.dp, periodMs = 1600)
                    else Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(6.dp))
                    Text(status, color = HW.InkSecondary, style = HWType.Subline)
                }
            }
        }
        Text(
            failed?.message ?: "Your Human Wonder satellite. Ask it what it sees: city lights, deserts, the Moon, the stars.",
            color = HW.IconMuted,
            style = HWType.ChatSub,
            modifier = Modifier.padding(top = 14.dp, bottom = 18.dp),
        )
        if (failed != null) {
            PrimaryButton("Try again", onClick = onRetry)
        } else {
            PrimaryButton("Talk to Orbit 412", onClick = onTalk)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OrbitMark(24.dp, active = false)
        Column(Modifier.weight(1f)) {
            Text(c.title, color = HW.Ink, style = HWType.RowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(transmissionTime(c.updatedAt), color = HW.InkTertiary, style = HWType.ChatMeta, maxLines = 1)
        }
        HwIcon(R.drawable.ic_chevron_right, HW.InkTertiary, 20.dp)
    }
}

/** A 40dp question chip: sparkle + 14/20 text on black 35% with a white 16% hairline. */
@Composable
private fun AskChip(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .widthIn(max = 250.dp)
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HwIcon(R.drawable.ic_sparkle, HW.Primary, 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(text, color = HW.Chip, style = HWType.Chip, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
