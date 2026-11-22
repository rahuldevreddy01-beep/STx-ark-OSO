package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StarkUiState
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

private data class VaultPage(
    val id: String,
    val code: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isCrimson: Boolean = false
)

@Composable
fun VaultScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onBack: () -> Unit = {},
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    val cards = remember(state.preferences.vaultNotes.size, state.vaultTimerRunning, state.vaultTimerSeconds) {
        listOf(
            VaultPage("notes", "01", "ENCRYPTED NOTES", "DATASTORE LOCAL", "${state.preferences.vaultNotes.size} SAVED"),
            VaultPage("timer", "02", "TACTICAL TIMER", "CHRONO COUNTDOWN", if (state.vaultTimerRunning) "${state.vaultTimerSeconds}S" else "STANDBY", state.vaultTimerRunning),
            VaultPage("compose", "03", "NEW VAULT ENTRY", "SAVE LOCAL", "WRITE")
        )
    }

    val safeIndex = focusedIndex.coerceIn(0, cards.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { cards.size })
    LaunchedEffect(safeIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != safeIndex) {
            pagerState.scrollToPage(safeIndex)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != safeIndex) {
            onSelectIndex(pagerState.settledPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xD8050A12))
                .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HudCrimsonSubtle)
                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("half_dial_center_back"),
                contentAlignment = Alignment.Center
            ) {
                Text("< BACK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("VAULT", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudIce)
                Text("${cards[safeIndex].code} // ${cards[safeIndex].subtitle}", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = HudCyan)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex - 1 + cards.size) % cards.size) }
                        .testTag("carousel_prev_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous vault card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex + 1) % cards.size) }
                        .testTag("carousel_next_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next vault card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cards.forEachIndexed { idx, c ->
                val sel = idx == safeIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sel) HudCyanSubtle else Color(0x8806101B))
                        .border(1.dp, if (sel) HudCyan else HudDivider, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${c.code} ${c.id.uppercase()}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (sel) HudIce else HudSilver, maxLines = 1)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            key = { cards[it].id },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            val card = cards[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEB040911))
                    .border(1.2.dp, if (card.isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
                    .then(if (page == pagerState.currentPage) Modifier.testTag("carousel_card_${card.id}") else Modifier),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(card.title, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HudIce)
                        Text("${card.code} // ${card.subtitle}", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudCyan)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (card.isCrimson) HudCrimsonSubtle else HudCyanSubtle)
                            .border(0.8.dp, if (card.isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(card.badge, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = HudIce)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HudCyan.copy(alpha = 0.22f)))

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (card.id) {
                        "notes" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${state.preferences.vaultNotes.size} PERSISTED ENTRIES", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = HudSilver)
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCyanSubtle)
                                        .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                        .clickable { onSelectIndex(2) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Add, "New note", tint = HudCyan, modifier = Modifier.size(14.dp))
                                        Text("NEW NOTE", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = HudIce)
                                    }
                                }
                            }

                            if (state.preferences.vaultNotes.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x5506101B))
                                        .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("VAULT EMPTY — SWIPE TO 03 COMPOSE TO LOG", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = HudSlate)
                                }
                            } else {
                                state.preferences.vaultNotes.forEach { note ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x6606101B))
                                            .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${note.title} · ${note.timestampLabel}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
                                            Text(note.body, style = MaterialTheme.typography.bodyMedium, color = HudIce)
                                        }
                                        IconButton(onClick = { onDeleteNote(note.id) }) {
                                            Icon(Icons.Default.Delete, "Delete note", tint = HudCrimson, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        "timer" -> {
                            Text(
                                text = if (state.vaultTimerRunning) "%02d:%02d".format(state.vaultTimerSeconds / 60, state.vaultTimerSeconds % 60) else "00:00",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp,
                                color = if (state.vaultTimerRunning) HudCrimson else HudCyan
                            )
                            Text(if (state.vaultTimerRunning) "TACTICAL COUNTDOWN ACTIVE" else "SELECT DURATION TO ARM", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = if (state.vaultTimerRunning) HudCrimson else HudSlate)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(60 to "1M", 300 to "5M", 600 to "10M", 900 to "15M").forEach { (sec, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HudCyanSubtle)
                                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                            .clickable { onStartTimer(sec) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCrimsonSubtle)
                                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onStopTimer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("STOP / RESET CHRONO", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                            }
                        }

                        "compose" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF040A12))
                                    .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (noteTitle.isEmpty()) Text("ENTRY TITLE…", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                                BasicTextField(
                                    value = noteTitle,
                                    onValueChange = { noteTitle = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 14.sp, color = HudIce),
                                    cursorBrush = SolidColor(HudCyan),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF040A12))
                                    .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                if (noteBody.isEmpty()) Text("ENCRYPTED NOTE BODY…", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                                BasicTextField(
                                    value = noteBody,
                                    onValueChange = { noteBody = it },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (noteTitle.isNotBlank() || noteBody.isNotBlank()) {
                                            onAddNote(noteTitle, noteBody)
                                            noteTitle = ""
                                            noteBody = ""
                                            onSelectIndex(0)
                                        }
                                    }),
                                    textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 14.sp, color = HudIce),
                                    cursorBrush = SolidColor(HudCyan),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (noteTitle.isNotBlank() || noteBody.isNotBlank()) {
                                            onAddNote(noteTitle, noteBody)
                                            noteTitle = ""
                                            noteBody = ""
                                            onSelectIndex(0)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("SAVE TO LOCAL VAULT", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce)
                            }
                        }
                    }
                }
            }
        }
    }
}
