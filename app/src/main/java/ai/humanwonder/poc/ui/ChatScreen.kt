package ai.humanwonder.poc.ui

import ai.humanwonder.poc.ChatMessage
import ai.humanwonder.poc.UiState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val SUGGESTIONS = listOf(
    "Why is the night sky dark?",
    "How far away is the Moon?",
    "What is a satellite?",
    "Why do stars twinkle?",
)

@Composable
fun ChatScreen(
    state: UiState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onNewChat: () -> Unit,
    onDeleteModel: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Follow the newest text as it streams in.
    val lastLength = state.messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(state.messages.size, lastLength) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
    }

    fun submit(text: String) {
        if (text.isNotBlank() && !state.generating) {
            onSend(text)
            input = ""
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill {
                Text("Human Wonder", color = HW.OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(" ✦", color = HW.Primary, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Pill(borderAlpha = 0.25f) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(HW.Primary),
                )
                Text("  ON-DEVICE", color = HW.Primary, fontFamily = HW.Mono, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Box {
                Text(
                    "⋮",
                    color = HW.OnSurfaceVariant,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { menuOpen = true }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("New chat") }, onClick = { menuOpen = false; onNewChat() })
                    DropdownMenuItem(text = { Text("About this model") }, onClick = { menuOpen = false; showAbout = true })
                    DropdownMenuItem(text = { Text("Delete model…") }, onClick = { menuOpen = false; confirmDelete = true })
                }
            }
        }

        // Conversation
        Box(Modifier.weight(1f)) {
            if (state.messages.isEmpty()) {
                EmptyState(onPick = ::submit)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.messages, key = { it.id }) { MessageBubble(it) }
                }
            }
        }

        // Prompt box
        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(HW.SurfaceLow.copy(alpha = 0.92f))
                .border(1.dp, HW.OutlineVariant, RoundedCornerShape(26.dp))
                .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = 24.dp)) {
                if (input.isEmpty()) {
                    Text("What shall we ask the cosmos tonight?", color = HW.Outline, fontSize = 16.sp)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = HW.OnSurface, fontSize = 16.sp, lineHeight = 22.sp),
                    cursorBrush = SolidColor(HW.Primary),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { submit(input) }),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel(
                    buildString {
                        append(state.modelLabel)
                        append(" · offline")
                        state.tokensPerSecond?.let { append(" · %.1f tok/s".format(Locale.ROOT, it)) }
                    },
                    color = HW.Outline,
                    modifier = Modifier.weight(1f),
                )
                val canSend = input.isNotBlank()
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (state.generating || canSend) HW.PrimaryContainer else HW.SurfaceHigh)
                        .clickable { if (state.generating) onStop() else submit(input) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (state.generating) "■" else "↑",
                        color = if (state.generating || canSend) HW.OnPrimaryContainer else HW.Outline,
                        fontSize = if (state.generating) 14.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.size(10.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete the model?") },
            text = { Text("This frees about 430 MB. You'll be asked before it downloads again.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteModel() }) { Text("Delete", color = HW.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            containerColor = HW.SurfaceHigh,
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About this model") },
            text = {
                Text(
                    "${state.modelLabel}, running with llama.cpp on this phone's CPU. " +
                        "Nothing you type leaves the device.\n\n" +
                        "It is a very small model, so it can be wrong about facts. " +
                        "Treat its answers as a starting point for wonder, not as the truth.",
                )
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } },
            containerColor = HW.SurfaceHigh,
        )
    }
}

@Composable
private fun EmptyState(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Talk to the\ninfinite sky",
            color = HW.OnSurface,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            "Ask anything. The answer is written by a small\nmodel on this phone, with no internet.",
            color = HW.OnSurfaceVariant.copy(alpha = 0.85f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(24.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SUGGESTIONS) { s ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HW.SurfaceLowest.copy(alpha = 0.6f))
                        .border(1.dp, HW.OutlineVariant, RoundedCornerShape(50))
                        .clickable { onPick(s) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✦ ", color = HW.Primary, fontSize = 12.sp)
                    Text(s, color = HW.OnSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    if (message.fromUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SelectionContainer {
                Text(
                    message.text,
                    color = HW.OnSurface,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                        .background(HW.SurfaceHigh)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                )
            }
        }
    } else {
        Row(Modifier.fillMaxWidth()) {
            Text("✦", color = HW.Primary, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp, end = 10.dp))
            if (message.text.isEmpty() && !message.complete) {
                Text(
                    "Listening to the sky…",
                    color = HW.Outline,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.sp,
                )
            } else {
                SelectionContainer {
                    Text(message.text, color = HW.OnSurface, fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun Pill(borderAlpha: Float = 0.12f, content: @Composable () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(HW.SurfaceLow.copy(alpha = 0.6f))
            .border(1.dp, HW.Primary.copy(alpha = borderAlpha), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
