package com.homecontrol.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homecontrol.app.data.CommandEntity
import com.homecontrol.app.data.CommandKind
import com.homecontrol.app.data.DeviceWithCommands
import com.homecontrol.app.ui.components.DeviceIcons
import com.homecontrol.app.ui.components.EmptyState
import com.homecontrol.app.ui.components.PressableBox
import com.homecontrol.app.ui.components.ltr
import com.homecontrol.app.ui.theme.AppColors

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    snackbar: SnackbarHostState,
    onOpenSettings: () -> Unit,
    onOpenDevices: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var showExplain by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshSim() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.confirmSend() else vm.denyPermission()
    }

    fun hasPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    fun proceed(explained: Boolean) {
        when {
            !explained -> showExplain = true
            !hasPermission() -> permissionLauncher.launch(Manifest.permission.SEND_SMS)
            else -> vm.confirmSend()
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            val result = snackbar.showSnackbar(
                message = e.text,
                actionLabel = e.action?.label,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                when (e.action) {
                    EventAction.OPEN_SETTINGS -> onOpenSettings()
                    EventAction.OPEN_APP_SETTINGS -> context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                    )
                    null -> Unit
                }
            }
        }
    }

    fun feedback() {
        if (state.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(168.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroCard(state, onFix = onOpenSettings)
        }

        if (state.totalDevices > 0 && state.categories.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { vm.selectCategory(null) },
                            label = { Text("همه") }
                        )
                    }
                    items(state.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat.id,
                            onClick = { vm.selectCategory(cat.id) },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        }

        if (state.devices.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = Icons.Filled.Home,
                    title = if (state.totalDevices == 0) "هنوز وسیله‌ای اضافه نشده" else "در این دسته وسیله‌ای نیست",
                    subtitle = "از بخش «وسایل» می‌توانید وسیله جدید بسازید و متن پیامک هر دکمه را تعیین کنید."
                ) {
                    Button(onClick = onOpenDevices) { Text("رفتن به مدیریت وسایل") }
                }
            }
        } else {
            items(state.devices, key = { it.device.id }) { item ->
                DeviceCard(
                    item = item,
                    categoryName = state.categories.firstOrNull { it.id == item.device.categoryId }?.name,
                    onCommand = { cmd ->
                        feedback()
                        vm.prepare(item.device, cmd)
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    val p = pending
    if (p != null && !showExplain) {
        AlertDialog(
            onDismissRequest = vm::cancelPending,
            icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
            title = { Text("تأیید ارسال فرمان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("وسیله", p.device.name)
                    InfoRow("فرمان", p.command.label)
                    InfoRow("شماره مقصد", ltr(p.number))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = p.command.smsText,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                    Text(
                        "این پیامک از سیم‌کارت گوشی شما ارسال می‌شود و ممکن است طبق تعرفه اپراتور هزینه داشته باشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { proceed(state.smsExplained) }) { Text("ارسال") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelPending) { Text("انصراف") }
            }
        )
    }

    if (showExplain) {
        AlertDialog(
            onDismissRequest = { showExplain = false; vm.cancelPending() },
            title = { Text("قبل از اولین ارسال") },
            text = {
                Text(
                    "HomeControl برای کنترل وسایل، پیامک را مستقیماً از سیم‌کارت شما به شماره‌ای که خودتان " +
                        "تعیین کرده‌اید می‌فرستد. به همین دلیل اندروید مجوز «ارسال پیامک» را از شما می‌خواهد.\n\n" +
                        "• هیچ پیامکی بدون تأیید شما ارسال نمی‌شود.\n" +
                        "• هر پیامک ممکن است هزینه داشته باشد.\n" +
                        "• برنامه به اینترنت نیاز ندارد."
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.markExplained()
                    showExplain = false
                    proceed(true)
                }) { Text("متوجه شدم، ادامه") }
            },
            dismissButton = {
                TextButton(onClick = { showExplain = false; vm.cancelPending() }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HeroCard(state: HomeUiState, onFix: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF3F5BFF), Color(0xFF12B5A5))))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Home, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "HomeControl",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "کنترل هوشمند خانه با پیامک",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            StatusPill(state.ready, onClick = if (state.ready == ReadyState.NO_NUMBER) onFix else null)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "شماره مقصد",
                    if (state.destination.isBlank()) "—" else ltr(state.destination),
                    Modifier.weight(1f)
                )
                StatTile("تعداد وسایل", state.totalDevices.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusPill(ready: ReadyState, onClick: (() -> Unit)?) {
    val (text, dot) = when (ready) {
        ReadyState.READY -> "آماده ارسال" to AppColors.success
        ReadyState.NO_NUMBER -> "شماره مقصد ثبت نشده (برای تنظیم بزنید)" to AppColors.warning
        ReadyState.NO_SIM -> "سیم‌کارت یافت نشد" to Color(0xFFFF6B81)
    }
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.22f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceCard(
    item: DeviceWithCommands,
    categoryName: String?,
    onCommand: (CommandEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val device = item.device
    val accent = Color(device.colorHex)
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    val commands = item.commands.sortedBy { it.sortOrder }
    val on = commands.firstOrNull { it.kind == CommandKind.ON }
    val off = commands.firstOrNull { it.kind == CommandKind.OFF }
    val extras = commands.filter { it != on && it != off }
    val onContent = if (accent.luminance() > 0.45f) Color(0xFF111827) else Color.White

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.26f), scheme.surface)))
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .padding(14.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.75f), accent.copy(alpha = 0.18f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(DeviceIcons.get(device.iconKey), null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            device.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (categoryName != null) {
            Text(categoryName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))

        if (on != null || off != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (on != null) {
                    CommandButton(
                        cmd = on,
                        brush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f))),
                        contentColor = onContent,
                        modifier = Modifier.weight(1f)
                    ) { onCommand(on) }
                }
                if (off != null) {
                    CommandButton(
                        cmd = off,
                        brush = SolidColor(scheme.surfaceVariant),
                        contentColor = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    ) { onCommand(off) }
                }
            }
        }

        if (extras.isNotEmpty()) {
            if (on != null || off != null) Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                extras.forEach { cmd ->
                    PressableBox(
                        onClick = { onCommand(cmd) },
                        background = SolidColor(accent.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(cmd.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandButton(
    cmd: CommandEntity,
    brush: Brush,
    contentColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    PressableBox(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        background = brush
    ) {
        Text(
            cmd.label,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
