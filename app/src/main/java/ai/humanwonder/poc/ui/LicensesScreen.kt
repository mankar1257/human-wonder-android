package ai.humanwonder.poc.ui

import ai.humanwonder.poc.R
import ai.humanwonder.poc.utils.FileUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Friendly titles for the notices bundled under assets/licenses. */
private val LICENSE_TITLES = mapOf(
    "NOTICE.txt" to "Human Wonder notices",
    "LICENSE-Apache-2.0.txt" to "Apache License 2.0",
    "LICENSE-llama.cpp.txt" to "MIT License",
    "NOTICE-model-and-icons.txt" to "Bundled data and icons",
    "OFL-geist.txt" to "Geist typeface (SIL Open Font License)",
    "OFL-jetbrainsmono.txt" to "JetBrains Mono typeface (SIL Open Font License)",
    "OFL-newsreader.txt" to "Newsreader typeface (SIL Open Font License)",
)

/** "Open-source licenses": every notice bundled with Human Wonder, as written. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val notices by produceState(initialValue = emptyList<Pair<String, String>>()) {
        value = withContext(Dispatchers.IO) { FileUtils.readAssetTexts(context, "licenses") }
            .sortedBy { (file, _) -> if (file == "NOTICE.txt") 0 else 1 }
    }
    Box(Modifier.fillMaxSize().background(HW.Space)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HwIconButton(R.drawable.ic_arrow_back, "Back", iconSize = 22.dp, onClick = onBack)
                Text(
                    "Open-source licenses",
                    color = Color.White,
                    style = HWType.HubTitle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(48.dp))
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            ) {
                items(notices, key = { it.first }) { (file, text) ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        SectionLabel(LICENSE_TITLES[file] ?: file.substringBeforeLast('.'), Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp))
                        Text(
                            text.trim(),
                            color = HW.IconMuted,
                            style = HWType.Subline,
                            modifier = Modifier.fillMaxWidth().glass().padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
