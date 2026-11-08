package ai.humanwonder.poc.ui

import ai.humanwonder.poc.Perspective
import ai.humanwonder.poc.R
import ai.humanwonder.poc.Tone
import ai.humanwonder.poc.data.repository.Profile
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val SegmentTrack = Color(0x12FFFFFF) // white 7%
private val SegmentOn = Color(0x24FFFFFF) // white 14%
private val ChipFill = Color(0x14FFFFFF) // white 8%, the chat's tone chip

/**
 * Profile & settings. Everything is kept on the phone and saved as it changes; a new name reaches
 * Orbit 412 with the next question (the persona is rebuilt from the profile).
 */
@Composable
fun ProfileScreen(
    profile: Profile,
    version: String,
    isValidEmail: (String) -> Boolean,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onCity: (String) -> Unit,
    onPerspective: (Perspective) -> Unit,
    onTone: (Tone) -> Unit,
    onReadAloud: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClearAll: () -> Unit,
    onLicenses: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var email by rememberSaveable { mutableStateOf(profile.email) }
    var city by rememberSaveable { mutableStateOf(profile.city) }
    var confirmClear by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val displayName = name.trim().ifBlank { "Wanderer" }
    val emailInvalid = email.isNotBlank() && !isValidEmail(email.trim())
    val perspective = Perspective.from(profile.perspective)
    val tone = Tone.from(profile.tone)

    Box(Modifier.fillMaxSize().background(HW.Space)) {
        // Top glow: an ellipse 90% wide and 36% tall at the top, violet .28 fading to teal .08.
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, 0f)
            val rx = size.width * 0.9f
            val ry = size.height * 0.36f
            withTransform({ scale(1f, ry / rx, pivot = center) }) {
                drawCircle(
                    Brush.radialGradient(
                        0f to Color(0x477C5CFF), 0.55f to Color(0x142FE6C8), 1f to Color.Transparent,
                        center = center, radius = rx,
                    ),
                    radius = rx, center = center,
                )
            }
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HwIconButton(R.drawable.ic_arrow_back, "Back", iconSize = 22.dp, onClick = onBack)
                Text(
                    "Profile & settings",
                    color = Color.White,
                    style = HWType.HubTitle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(48.dp))
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            ) {
                // Avatar: an 84dp disc in a 3dp teal-to-iris ring.
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(HW.Primary, HW.Iris)))
                            .padding(3.dp),
                    ) {
                        Box(
                            Modifier.size(84.dp).clip(CircleShape).background(HW.Sheet),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(initialOf(displayName), color = Color.White, style = HWType.AvatarInitial.capScale(), maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(displayName, color = Color.White, style = HWType.ProfileName, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        profile.email.ifBlank { "Your Human Wonder profile" },
                        color = HW.InkSecondary,
                        style = HWType.ChipStrong.copy(fontWeight = null),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                SectionLabel("You", Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().glass()) {
                    ProfileField(
                        label = "Name or callsign",
                        value = name,
                        placeholder = "Your name or callsign",
                        onValueChange = { v ->
                            name = v.take(40)
                            onName(name)
                        },
                        capitalization = KeyboardCapitalization.Words,
                        onDone = { focus.clearFocus() },
                    )
                    GlassDivider()
                    ProfileField(
                        label = "Email",
                        value = email,
                        placeholder = "you@example.com",
                        onValueChange = { v ->
                            email = v.take(80)
                            onEmail(email)
                        },
                        keyboardType = KeyboardType.Email,
                        helper = if (emailInvalid) "That doesn’t look like an email address yet." else "Optional. Only kept on this phone.",
                        helperIsError = emailInvalid,
                        onDone = { focus.clearFocus() },
                    )
                    GlassDivider()
                    ProfileField(
                        label = "Where you look up from",
                        value = city,
                        placeholder = "City or place, optional",
                        onValueChange = { v ->
                            city = v.take(60)
                            onCity(city)
                        },
                        capitalization = KeyboardCapitalization.Words,
                        onDone = { focus.clearFocus() },
                    )
                }

                SectionLabel("Orbit 412", Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().glass()) {
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                        Text("Point of view", color = HW.Ink, style = HWType.RowTitle)
                        Text("How Orbit 412 describes things", color = HW.InkTertiary, style = HWType.Subline)
                        Spacer(Modifier.height(12.dp))
                        PointOfView(perspective, onPerspective)
                    }
                    GlassDivider()
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                        Text("Tone", color = HW.Ink, style = HWType.RowTitle)
                        Text("How Orbit 412 sounds", color = HW.InkTertiary, style = HWType.Subline)
                        Spacer(Modifier.height(12.dp))
                        ToneChips(tone, onTone)
                    }
                    GlassDivider()
                    SwitchRow("Read answers aloud", "Orbit 412 speaks each answer", profile.readAloud, onReadAloud)
                    GlassDivider()
                    SwitchRow("Haptics", "A soft tap when an answer lands", profile.haptics, onHaptics)
                }

                SectionLabel("Your transmissions", Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().glass()) {
                    NavRow(R.drawable.ic_upload, "Export transmissions", "Save all conversations as a file", first = true, onClick = onExport)
                    NavRow(
                        R.drawable.ic_delete, "Clear all transmissions", "This can’t be undone",
                        color = HW.Error, onClick = { confirmClear = true },
                    )
                }

                SectionLabel("About", Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().glass()) {
                    NavRow(R.drawable.ic_info, "About Human Wonder", "Version $version", first = true, onClick = { showAbout = true })
                    NavRow(R.drawable.ic_info, "Open-source licenses", null, onClick = onLicenses)
                }

                Text(
                    "Human Wonder $version",
                    color = HW.InkTertiary,
                    style = HWType.ChatMeta,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = HW.Sheet,
            titleContentColor = Color.White,
            textContentColor = HW.InkSecondary,
            title = { Text("Clear all transmissions?", style = HWType.HubTitle) },
            text = {
                Text(
                    "Every conversation with Orbit 412 will be deleted from this phone. This can’t be undone.",
                    style = HWType.ChatSub,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("Clear all", color = HW.Error, style = HWType.ButtonLabel) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = HW.Ink, style = HWType.ButtonLabel) }
            },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = HW.Sheet,
            titleContentColor = Color.White,
            textContentColor = HW.InkSecondary,
            title = { Text("Human Wonder", style = HWType.HubTitle) },
            text = {
                Text(
                    "Version $version. Human Wonder gives the sky a voice: Orbit 412, a satellite passing over you tonight. " +
                        "Your profile and conversations stay on this phone.",
                    style = HWType.ChatSub,
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Close", color = HW.Primary, style = HWType.ButtonLabel) }
            },
        )
    }
}

/** A labelled field row (at least 64dp): label 13/18, input 16/24, optional helper 12/16. */
@Composable
private fun ProfileField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    helper: String? = null,
    helperIsError: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                focusRequester.requestFocus()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = HW.InkSecondary, style = HWType.Subline)
        Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(placeholder, color = HW.Placeholder, style = HWType.ChatInput, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = HWType.ChatInput.copy(color = Color.White),
                cursorBrush = SolidColor(HW.Primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = capitalization,
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = label },
            )
        }
        if (helper != null) {
            Text(helper, color = if (helperIsError) HW.Error else HW.InkTertiary, style = HWType.ChatMeta)
        }
    }
}

/** Orbit / Ground: two 44dp segments on a white 7% track; the active one is white 14%. */
@Composable
private fun PointOfView(selected: Perspective, onSelect: (Perspective) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(SegmentTrack)
            .padding(3.dp),
    ) {
        Perspective.entries.forEach { p ->
            val on = p == selected
            val color = if (on) Color.White else HW.InkSecondary
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(if (on) SegmentOn else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(p) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // As in the chat composer: the active Orbit icon is teal.
                HwIcon(
                    if (p == Perspective.Orbit) R.drawable.ic_satellite else R.drawable.ic_location,
                    if (on && p == Perspective.Orbit) HW.Primary else color, 18.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(p.label, color = color, style = HWType.SegmentLabel, maxLines = 1)
            }
        }
    }
}

/** Four wrapping 40dp chips in the chat's chip style: white 8%, the active one white 14% with a teal sparkle. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToneChips(selected: Tone, onSelect: (Tone) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tone.entries.forEach { t ->
            val on = t == selected
            val color = if (on) Color.White else HW.InkSecondary
            Row(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(CircleShape)
                    .background(if (on) SegmentOn else ChipFill)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(t) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HwIcon(R.drawable.ic_sparkle, if (on) HW.Primary else color, 14.dp)
                Spacer(Modifier.width(6.dp))
                Text(t.label, color = color, style = HWType.ChipStrong, maxLines = 1)
            }
        }
    }
}

/** A switch row (at least 64dp); the whole row toggles. Checked track #83FFE5, thumb #00201A. */
@Composable
private fun SwitchRow(label: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = HW.Ink, style = HWType.RowTitle)
            Text(sub, color = HW.InkTertiary, style = HWType.Subline)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HW.OnAccent,
                checkedTrackColor = HW.Primary,
                checkedBorderColor = HW.Primary,
                uncheckedThumbColor = Color(0xBFFFFFFF),
                uncheckedTrackColor = Color(0x1AFFFFFF),
                uncheckedBorderColor = Color(0x59FFFFFF),
            ),
        )
    }
}

/** A row that opens something (at least 56dp): icon 22, label 16/22, optional sub-line, chevron. */
@Composable
private fun NavRow(
    icon: Int,
    label: String,
    sub: String?,
    first: Boolean = false,
    color: Color = HW.Ink,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(HW.Divider))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HwIcon(icon, if (color == HW.Ink) HW.IconMuted else color, 22.dp)
            Column(Modifier.weight(1f)) {
                Text(label, color = color, style = HWType.RowTitle)
                if (sub != null) Text(sub, color = HW.InkTertiary, style = HWType.Subline)
            }
            HwIcon(R.drawable.ic_chevron_right, HW.InkTertiary, 20.dp)
        }
    }
}
