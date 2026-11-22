package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardware.LauncherPermissionManager
import com.example.ui.StarkUiState
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily

private data class CameraPage(
    val id: String,
    val code: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isCrimson: Boolean = false
)

@Composable
fun CameraScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onBack: () -> Unit = {},
    onToggleTorch: () -> Unit,
    onFlipLens: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onPermissionGranted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPerm by remember {
        mutableStateOf(LauncherPermissionManager.hasCameraPermission(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = LauncherPermissionManager.hasCameraPermission(context)
                val changed = !hasCameraPerm && nowGranted
                hasCameraPerm = nowGranted
                if (changed) onPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val effective = granted || LauncherPermissionManager.hasCameraPermission(context)
        hasCameraPerm = effective
        if (effective) onPermissionGranted()
    }

    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val cards = remember(state.cameraFacingBack, state.cameraZoom, state.telemetry.flashlightOn) {
        listOf(
            CameraPage("optical", "01", "LIVE OPTICAL FEED", if (state.cameraFacingBack) "CAMERAX REAR" else "CAMERAX FRONT", "${"%.1f".format(state.cameraZoom)}X"),
            CameraPage("torch", "02", "OPTICAL LED TORCH", "ILLUMINATION HW", if (state.telemetry.flashlightOn) "ACTIVE" else "OFF", state.telemetry.flashlightOn),
            CameraPage("lens", "03", "OPTICAL SENSOR FLIP", "DUAL LENS SELECTOR", if (state.cameraFacingBack) "REAR" else "FRONT"),
            CameraPage("zoom", "04", "DIGITAL MAGNIFICATION", "RATIO CONTROL", "${"%.1f".format(state.cameraZoom)}X")
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
                Text("OPTICAL HUD", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudIce)
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
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous camera card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex + 1) % cards.size) }
                        .testTag("carousel_next_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next camera card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${c.code} ${c.id.uppercase()}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = if (sel) HudIce else HudSilver, maxLines = 1)
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

                when (card.id) {
                    "optical" -> {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (state.telemetry.flashlightOn) HudCrimsonSubtle else HudCyanSubtle)
                                        .border(1.dp, if (state.telemetry.flashlightOn) HudCrimson else HudCyanBorder, RoundedCornerShape(4.dp))
                                        .clickable(onClick = onToggleTorch)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.FlashlightOn, "Torch", tint = if (state.telemetry.flashlightOn) HudCrimson else HudCyan, modifier = Modifier.size(14.dp))
                                        Text("TORCH", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = HudIce)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HudCyanSubtle)
                                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                        .clickable(onClick = onFlipLens)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Cameraswitch, "Flip camera", tint = HudCyan, modifier = Modifier.size(14.dp))
                                        Text("FLIP", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = HudIce)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF03070C))
                                    .border(1.dp, HudDivider, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasCameraPerm) {
                                    val previewView = remember {
                                        PreviewView(context).apply {
                                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                            scaleType = PreviewView.ScaleType.FILL_CENTER
                                        }
                                    }
                                    DisposableEffect(state.cameraFacingBack, lifecycleOwner) {
                                        var disposed = false
                                        val cameraProviderFuture = runCatching { ProcessCameraProvider.getInstance(context) }.getOrNull()
                                        val executor = ContextCompat.getMainExecutor(context)
                                        cameraProviderFuture?.addListener({
                                            if (disposed) return@addListener
                                            runCatching {
                                                val provider = cameraProviderFuture.get()
                                                val preferred = if (state.cameraFacingBack) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                                                val fallback = if (state.cameraFacingBack) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                                                val selector = when {
                                                    runCatching { provider.hasCamera(preferred) }.getOrDefault(false) -> preferred
                                                    runCatching { provider.hasCamera(fallback) }.getOrDefault(false) -> fallback
                                                    else -> return@runCatching
                                                }
                                                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                                                provider.unbindAll()
                                                val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                                                boundCamera = cam
                                                val zState = cam.cameraInfo.zoomState.value
                                                val clamped = if (zState != null) state.cameraZoom.coerceIn(zState.minZoomRatio, zState.maxZoomRatio) else state.cameraZoom.coerceAtLeast(1f)
                                                cam.cameraControl.setZoomRatio(clamped)
                                            }
                                        }, executor)
                                        onDispose {
                                            disposed = true
                                            boundCamera = null
                                            if (cameraProviderFuture?.isDone == true) {
                                                runCatching { cameraProviderFuture.get().unbindAll() }
                                            }
                                        }
                                    }

                                    LaunchedEffect(state.cameraZoom, boundCamera) {
                                        runCatching {
                                            val cam = boundCamera ?: return@runCatching
                                            val zState = cam.cameraInfo.zoomState.value
                                            val clamped = if (zState != null) state.cameraZoom.coerceIn(zState.minZoomRatio, zState.maxZoomRatio) else state.cameraZoom.coerceAtLeast(1f)
                                            cam.cameraControl.setZoomRatio(clamped)
                                        }
                                    }

                                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f
                                        val r = size.minDimension * 0.22f
                                        drawCircle(HudCyan.copy(alpha = 0.55f), radius = r, center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
                                        drawLine(HudCrimson, Offset(cx - 14.dp.toPx(), cy), Offset(cx + 14.dp.toPx(), cy), 1.5.dp.toPx())
                                        drawLine(HudCrimson, Offset(cx, cy - 14.dp.toPx()), Offset(cx, cy + 14.dp.toPx()), 1.5.dp.toPx())
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(10.dp)) {
                                        Icon(Icons.Default.CameraAlt, "Camera access", tint = HudCyan, modifier = Modifier.size(28.dp))
                                        Text("CAMERA PERMISSION REQUIRED", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudIce)
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 42.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(HudCyanSubtle)
                                                .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                                .clickable { permLauncher.launch(Manifest.permission.CAMERA) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("GRANT CAMERA ACCESS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "torch" -> {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CameraSpecRow("HARDWARE FLASH", if (state.telemetry.flashlightAvailable) "AVAILABLE" else "EDGE GLOW MODE", if (state.telemetry.flashlightAvailable) HudEmerald else HudCyan)
                            CameraSpecRow("ILLUMINATION", if (state.telemetry.flashlightOn) "ACTIVE" else "STANDBY", if (state.telemetry.flashlightOn) HudCrimson else HudSlate)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (state.telemetry.flashlightOn) HudCrimsonSubtle else HudCyanSubtle)
                                    .border(1.dp, if (state.telemetry.flashlightOn) HudCrimson else HudCyan, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onToggleTorch)
                                    .testTag("camera_toggle_torch_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.FlashlightOn, "Toggle torch", tint = if (state.telemetry.flashlightOn) HudCrimson else HudCyan, modifier = Modifier.size(18.dp))
                                    Text(if (state.telemetry.flashlightOn) "DISABLE TORCH" else "ENGAGE TORCH", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = HudIce)
                                }
                            }
                        }
                    }

                    "lens" -> {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CameraSpecRow("ACTIVE SENSOR", if (state.cameraFacingBack) "REAR PRIMARY" else "FRONT OPTICAL", HudCyan)
                            CameraSpecRow("MAGNIFICATION", "${"%.1f".format(state.cameraZoom)}X", HudIce)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCyanSubtle)
                                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                    .clickable(onClick = onFlipLens)
                                    .testTag("camera_flip_lens_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Cameraswitch, "Flip camera", tint = HudCyan, modifier = Modifier.size(18.dp))
                                    Text(if (state.cameraFacingBack) "SWITCH TO FRONT LENS" else "SWITCH TO REAR LENS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HudCrimsonSubtle)
                                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                                    .clickable { onSelectIndex(0) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OPEN LIVE FEED >", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                            }
                        }
                    }

                    "zoom" -> {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${"%.1f".format(state.cameraZoom)}X", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = HudCyan)
                            Slider(
                                value = state.cameraZoom,
                                onValueChange = { z ->
                                    onZoomChange(z)
                                    runCatching {
                                        val cam = boundCamera ?: return@runCatching
                                        val zState = cam.cameraInfo.zoomState.value
                                        val clamped = if (zState != null) z.coerceIn(zState.minZoomRatio, zState.maxZoomRatio) else z.coerceAtLeast(1f)
                                        cam.cameraControl.setZoomRatio(clamped)
                                    }
                                },
                                valueRange = 1f..5f,
                                colors = SliderDefaults.colors(thumbColor = HudCyan, activeTrackColor = HudCyan)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1.0f, 2.0f, 3.5f, 5.0f).forEach { ratio ->
                                    val selected = kotlin.math.abs(state.cameraZoom - ratio) < 0.25f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (selected) HudCrimsonSubtle else HudCyanSubtle)
                                            .border(1.dp, if (selected) HudCrimson else HudCyanBorder, RoundedCornerShape(4.dp))
                                            .clickable { onZoomChange(ratio) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${"%.1f".format(ratio)}X", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x5506101B))
                                    .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                                    .clickable { onSelectIndex(0) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("VIEW IN OPTICAL FEED >", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSpecRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate, maxLines = 1)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}
