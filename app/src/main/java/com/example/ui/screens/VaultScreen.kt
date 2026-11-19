package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.HolographicCardCarousel
import com.example.ui.components.HolographicCardHeader
import com.example.ui.components.HolographicSideCardFooter
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

private data class VaultDeckCard(
    val id: String,
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
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    val cards = remember(
        state.preferences.vaultNotes.size,
        state.vaultTimerRunning,
        state.vaultTimerSeconds
    ) {
        listOf(
            VaultDeckCard(
                id = "notes",
                title = "ENCRYPTED NOTES",
                subtitle = "01 VAULT // DATASTORE",
                badge = "${state.preferences.vaultNotes.size} SAVED"
            ),
            VaultDeckCard(
                id = "timer",
                title = "TACTICAL TIMER",
                subtitle = "02 COUNTDOWN // CHRONO",
                badge = if (state.vaultTimerRunning) "${state.vaultTimerSeconds}S" else "STANDBY",
                isCrimson = state.vaultTimerRunning
            ),
            VaultDeckCard(
                id = "compose",
                title = "NEW VAULT ENTRY",
                subtitle = "03 COMPOSER // SAVE LOCAL",
                badge = "WRITE"
            )
        )
    }

    HolographicCardCarousel(
        items = cards,
        focusedIndex = focusedIndex,
        onSelectIndex = onSelectIndex,
        itemKey = { _, card -> card.id },
        isCrimsonAccent = { _, card, _ -> card.isCrimson },
        modifier = modifier
    ) { idx, card, isCenter, isCompactStage, _, relOffset ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isCenter) Modifier.testTag("carousel_card_${card.id}") else Modifier),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HolographicCardHeader(
                index = idx,
                totalCount = cards.size,
                moduleLabel = "VAULT",
                title = card.title,
                subtitle = card.subtitle,
                badge = card.badge,
                isCenter = isCenter,
                isCompactStage = isCompactStage,
                crimsonBadge = card.isCrimson
            )

            if (isCenter) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (card.id) {
                        "notes" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.preferences.vaultNotes.size} PERSISTED ENTRIES",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.5.sp,
                                    color = HudSilver
                                )
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
                                    Text(
                                        text = "VAULT EMPTY — SWIPE TO 03 NEW NOTE TO LOG",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        color = HudSlate
                                    )
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
                                fontSize = if (isCompactStage) 26.sp else 34.sp,
                                color = if (state.vaultTimerRunning) HudCrimson else HudCyan
                            )
                            Text(
                                text = if (state.vaultTimerRunning) "TACTICAL COUNTDOWN ACTIVE" else "SELECT DURATION TO ARM",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.5.sp,
                                color = if (state.vaultTimerRunning) HudCrimson else HudSlate
                            )
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
                                    .heightIn(min = 56.dp)
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
            } else {
                Spacer(modifier = Modifier.weight(1f))
                HolographicSideCardFooter(relOffset = relOffset)
            }
        }
    }
}
