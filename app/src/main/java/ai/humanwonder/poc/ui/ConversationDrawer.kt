package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import ai.humanwonder.poc.data.local.entities.Conversation
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/** Sheet colour, scrim and hairline from the preview's side menu. */
private val SheetEdge = Color(0x0FFFFFFF) // white 6%
private val SheetScrim = Color(0x99000000) // black 60%
private val ItemFill = Color(0x14FFFFFF) // white 8%, the chat's chip fill
private val SelectedFill = Color(0x1F83FFE5) // teal 12%
private val ProfileEdge = Color(0x14FFFFFF) // white 8%

/** The side menu's conversation groups, newest first. */
private fun groupByDay(list: List<Conversation>, today: LocalDate = LocalDate.now()): List<Pair<String, List<Conversation>>> {
    val zone = ZoneId.systemDefault()
    fun dayOf(c: Conversation) = Instant.ofEpochMilli(c.updatedAt).atZone(zone).toLocalDate()
    val todayList = list.filter { dayOf(it) == today }
    val yesterdayList = list.filter { dayOf(it) == today.minusDays(1) }
    val earlier = list.filter { dayOf(it).isBefore(today.minusDays(1)) || dayOf(it).isAfter(today) }
    return listOf("Today" to todayList, "Yesterday" to yesterdayList, "Earlier" to earlier).filter { it.second.isNotEmpty() }
}

/**
 * The Gemini-style side menu used on Home and in the chat: a sheet min(320dp, screen − 56dp) wide
 * in #0B0D12 with 24dp end corners, over a black 60% scrim. It opens from the menu button only;
 * swiping or tapping the scrim closes it, and so does system back.
 */
@Composable
fun SideMenuDrawer(
    drawerState: DrawerState,
    name: String,
    conversations: List<Conversation>,
    query: String,
    onQuery: (String) -> Unit,
    selectedId: String?,
    onHome: () -> Unit,
    onNewConversation: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onProfile: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val closeThen: (() -> Unit) -> Unit = { action ->
        focus.clearFocus()
        scope.launch { drawerState.close() }
        action()
    }
    BackHandler(enabled = drawerState.isOpen) { closeThen {} }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        scrimColor = SheetScrim,
        drawerContent = {
            DrawerSheet(
                name = name,
                conversations = conversations,
                query = query,
                onQuery = onQuery,
                selectedId = selectedId,
                focus = focus,
                onHome = { closeThen(onHome) },
                onNewConversation = { closeThen(onNewConversation) },
                onOpen = { id -> closeThen { onOpen(id) } },
                onRename = onRename,
                onDelete = onDelete,
                onProfile = { closeThen(onProfile) },
            )
        },
        content = content,
    )
}

@Composable
private fun DrawerSheet(
    name: String,
    conversations: List<Conversation>,
    query: String,
    onQuery: (String) -> Unit,
    selectedId: String?,
    focus: FocusManager,
    onHome: () -> Unit,
    onNewConversation: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onProfile: () -> Unit,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val width = minOf(320.dp, screenWidth - 56.dp)
    var renaming by remember { mutableStateOf<Conversation?>(null) }
    var deleting by remember { mutableStateOf<Conversation?>(null) }

    ModalDrawerSheet(
        modifier = Modifier
            .width(width)
            // 1dp white 6% hairline along the straight part of the end edge.
            .drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                val corner = 24.dp.toPx()
                val x = size.width - stroke / 2
                drawLine(SheetEdge, Offset(x, corner), Offset(x, size.height - corner), strokeWidth = stroke)
            },
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = HW.Sheet,
        drawerContentColor = HW.Ink,
        // The glass tint runs edge to edge (under the status and navigation bars); the content
        // keeps the usual insets itself.
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        // The shared glass tint over the opaque sheet, so the menu belongs to the same app.
        Column(Modifier.fillMaxSize().background(GlassBrush).windowInsetsPadding(DrawerDefaults.windowInsets)) {
            Box(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) { BrandLockup() }

            Column(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MenuItem(R.drawable.ic_home, "Home", HW.IconMuted, Color.Transparent, onHome)
                MenuItem(R.drawable.ic_add, "New conversation", HW.Primary, ItemFill, onNewConversation)
            }

            SearchField(query, onQuery, focus)

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (conversations.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            if (query.isBlank()) "Your conversations with Orbit 412 will appear here."
                            else "No transmissions match “${query.trim()}”.",
                            color = HW.InkTertiary,
                            style = HWType.MenuLabel,
                            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp),
                        )
                    }
                }
                groupByDay(conversations).forEach { (label, group) ->
                    item(key = "label-$label") {
                        SectionLabel(label, Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 6.dp))
                    }
                    items(group, key = { it.id }) { c ->
                        ConversationRow(
                            c,
                            selected = c.id == selectedId,
                            onOpen = { onOpen(c.id) },
                            onRename = { renaming = c },
                            onDelete = { deleting = c },
                        )
                    }
                }
            }

            ProfileRow(name, onProfile)
        }
    }

    renaming?.let { c ->
        RenameDialog(
            initial = c.title,
            onDismiss = { renaming = null },
            onSave = { title ->
                onRename(c.id, title)
                renaming = null
            },
        )
    }
    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = HW.Sheet,
            titleContentColor = Color.White,
            textContentColor = HW.InkSecondary,
            title = { Text("Delete this transmission?", style = HWType.HubTitle) },
            text = { Text("“${c.title}” will be removed from this phone.", style = HWType.ChatSub) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(c.id)
                    deleting = null
                }) { Text("Delete", color = HW.Error, style = HWType.ButtonLabel) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel", color = HW.Ink, style = HWType.ButtonLabel) }
            },
        )
    }
}

/** A 48dp pill: Home, New conversation. */
@Composable
private fun MenuItem(icon: Int, label: String, tint: Color, fill: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(fill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HwIcon(icon, tint, 22.dp)
        Text(label, color = HW.Ink, style = HWType.MenuLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, focus: FocusManager) {
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ItemFill)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HwIcon(R.drawable.ic_search, HW.InkTertiary, 20.dp)
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search your transmissions",
                    color = HW.Placeholder,
                    style = HWType.MenuLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = HWType.MenuLabel.copy(color = Color.White),
                cursorBrush = SolidColor(HW.Primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search your transmissions" },
            )
        }
    }
}

/** A 48dp conversation pill. The open one is teal 12% with a "More options" button; long-press opens the same menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    c: Conversation,
    selected: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (selected) SelectedFill else Color.Transparent)
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = "More options",
                    onLongClick = { menu = true },
                    onClick = onOpen,
                )
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                c.title,
                color = if (selected) Color.White else HW.Chip,
                style = HWType.MenuLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
            )
            if (selected) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button) { menu = true },
                    contentAlignment = Alignment.Center,
                ) { HwIcon(R.drawable.ic_more_vert, HW.InkSecondary, 20.dp, contentDescription = "More options") }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Rename", style = HWType.MenuLabel) },
                onClick = {
                    menu = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = HW.Error, style = HWType.MenuLabel) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

/** The 72dp profile row at the bottom: avatar, name, "Profile & settings", chevron. */
@Composable
private fun ProfileRow(name: String, onClick: () -> Unit) {
    val displayName = name.trim().ifBlank { "Wanderer" }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .drawBehind { drawLine(ProfileEdge, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialAvatar(displayName, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(displayName, color = Color.White, style = HWType.MenuLabel.copy(fontWeight = HWType.ChatLabel.fontWeight), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Profile & settings", color = HW.InkSecondary, style = HWType.ChatMeta, maxLines = 1)
        }
        HwIcon(R.drawable.ic_chevron_right, HW.InkTertiary, 20.dp)
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HW.Sheet,
        titleContentColor = Color.White,
        textContentColor = HW.Ink,
        title = { Text("Rename transmission", style = HWType.HubTitle) },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ItemFill)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    singleLine = true,
                    textStyle = HWType.ChatInput.copy(color = Color.White),
                    cursorBrush = SolidColor(HW.Primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (title.isNotBlank()) onSave(title) }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Transmission name" },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title) }, enabled = title.isNotBlank()) {
                Text("Save", color = if (title.isNotBlank()) HW.Primary else HW.InkDisabled, style = HWType.ButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = HW.Ink, style = HWType.ButtonLabel) }
        },
    )
}
