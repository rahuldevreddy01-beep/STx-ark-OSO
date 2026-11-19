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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.InstalledApp
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
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

private data class SearchDeckCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isCrimson: Boolean = false
)

@Composable
fun SearchScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onLaunchFullscreen: (InstalledApp) -> Unit,
    onWebSearch: (String) -> Unit,
    onAskStxAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val matches = remember(state.searchQuery, state.installedApps) {
        val q = state.searchQuery.trim().lowercase()
        if (q.isEmpty()) state.installedApps.take(8)
        else state.installedApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }.take(12)
    }

    var aiPrompt by remember { mutableStateOf("") }

    val cards = remember(state.installedApps.size, matches.size) {
        listOf(
            SearchDeckCard(
                id = "apps_q",
                title = "INSTALLED APPS INDEX",
                subtitle = "01 APP QUERY // ${matches.size} MATCHES",
                badge = "${state.installedApps.size} APPS"
            ),
            SearchDeckCard(
                id = "web_q",
                title = "BROWSER WEB UPLINK",
                subtitle = "02 WEB LINK // EXTERNAL SEARCH",
                badge = "BROWSER"
            ),
            SearchDeckCard(
                id = "stx_q",
                title = "ASK STX AI CORE",
                subtitle = "03 GEMINI // NEURAL COMMAND",
                badge = "GEMINI",
                isCrimson = true
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
                moduleLabel = "SEARCH",
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
                        "apps_q" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF040A12))
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Search, "Search", tint = HudCyan, modifier = Modifier.size(18.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (state.searchQuery.isEmpty()) {
                                        Text(stringResource(R.string.search_apps_hint), fontFamily = JetBrainsMonoFontFamily, fontSize = 10.5.sp, color = HudSlate)
                                    }
                                    BasicTextField(
                                        value = state.searchQuery,
                                        onValueChange = onQueryChange,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            val top = matches.firstOrNull()
                                            if (top != null) onLaunchFullscreen(top)
                                            else onWebSearch(state.searchQuery)
                                        }),
                                        textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 15.sp, color = HudIce),
                                        cursorBrush = SolidColor(HudCyan),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("search_module_input")
                                    )
                                }
                            }

                            matches.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x6606101B))
                                        .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
                                        .clickable { onLaunchFullscreen(app) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = app.label.uppercase(),
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 11.5.sp,
                                        color = HudIce,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(app.categoryTag, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = HudCyan)
                                }
                            }
                        }

                        "web_q" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF040A12))
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Public, "Web search", tint = HudCyan, modifier = Modifier.size(18.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (state.searchQuery.isEmpty()) {
                                        Text("ENTER WEB QUERY OR URL…", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                                    }
                                    BasicTextField(
                                        value = state.searchQuery,
                                        onValueChange = onQueryChange,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { onWebSearch(state.searchQuery) }),
                                        textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 15.sp, color = HudIce),
                                        cursorBrush = SolidColor(HudCyan),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .clickable { onWebSearch(state.searchQuery) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OPEN BROWSER SEARCH", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce)
                            }
                        }

                        "stx_q" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF040A12))
                                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (aiPrompt.isEmpty()) {
                                    Text("COMMAND OR QUESTION FOR STX AI…", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                                }
                                BasicTextField(
                                    value = aiPrompt,
                                    onValueChange = { aiPrompt = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        onAskStxAi(aiPrompt.ifBlank { state.searchQuery }.ifBlank { "system brief" })
                                    }),
                                    textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 14.sp, color = HudIce),
                                    cursorBrush = SolidColor(HudCrimson),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("SYSTEM BRIEF", "SENSOR STATUS", "RF TELEMETRY").forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x5506101B))
                                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                            .clickable { onAskStxAi(preset) }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(preset, fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = HudCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCrimsonSubtle)
                                    .border(1.dp, HudCrimson, RoundedCornerShape(4.dp))
                                    .clickable {
                                        onAskStxAi(aiPrompt.ifBlank { state.searchQuery }.ifBlank { "system brief" })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("TRANSMIT TO STX AI", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce)
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
