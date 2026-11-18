package com.homecontrol.app.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homecontrol.app.data.CommandKind
import com.homecontrol.app.ui.components.DeviceColors
import com.homecontrol.app.ui.components.DeviceIcons
import com.homecontrol.app.util.SmsUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: EditorViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                if (vm.isNew) "وسیله جدید" else "ویرایش وسیله",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard("اطلاعات وسیله") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = vm::setName,
                    label = { Text("نام وسیله") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard("آیکون") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceIcons.all.forEach { icon ->
                        val selected = icon.key == state.iconKey
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Color(state.color).copy(alpha = 0.25f) else scheme.surfaceVariant)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) Color(state.color) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { vm.setIcon(icon.key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon.vector,
                                contentDescription = icon.label,
                                tint = if (selected) Color(state.color) else scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            SectionCard("رنگ") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeviceColors.forEach { colorValue ->
                        val selected = colorValue == state.color
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(colorValue))
                                .border(if (selected) 3.dp else 0.dp, scheme.onSurface, CircleShape)
                                .clickable { vm.setColor(colorValue) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            SectionCard("دسته‌بندی") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = { vm.setCategory(null) },
                        label = { Text("بدون دسته") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = state.categoryId == cat.id,
                            onClick = { vm.setCategory(cat.id) },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            SectionCard("شماره مقصد") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("استفاده از شماره اصلی (تنظیمات)", Modifier.weight(1f))
                    Switch(checked = state.useMainNumber, onCheckedChange = vm::setUseMainNumber)
                }
                if (!state.useMainNumber) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.destNumber,
                        onValueChange = vm::setDest,
                        label = { Text("شماره اختصاصی این وسیله") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text("فرمان‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.commands.forEachIndexed { index, draft ->
                CommandCard(
                    index = index,
                    draft = draft,
                    canDelete = state.commands.size > 1,
                    onChange = vm::updateCommand,
                    onDelete = { vm.removeCommand(draft.key) }
                )
            }
            OutlinedButton(onClick = vm::addCommand, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("افزودن فرمان")
            }
            Spacer(Modifier.height(4.dp))
        }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (state.error != null) {
                Text(
                    state.error ?: "",
                    color = scheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("ذخیره", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun CommandCard(
    index: Int,
    draft: CommandDraft,
    canDelete: Boolean,
    onChange: (CommandDraft) -> Unit,
    onDelete: () -> Unit
) {
    val parts = SmsUtils.partsCount(draft.text)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "فرمان ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف فرمان", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    CommandKind.ON to "روشن",
                    CommandKind.OFF to "خاموش",
                    CommandKind.CUSTOM to "سفارشی"
                ).forEach { (kind, label) ->
                    FilterChip(
                        selected = draft.kind == kind,
                        onClick = { onChange(draft.copy(kind = kind)) },
                        label = { Text(label) }
                    )
                }
            }
            OutlinedTextField(
                value = draft.label,
                onValueChange = { onChange(draft.copy(label = it)) },
                label = { Text("نام دکمه") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.text,
                onValueChange = { onChange(draft.copy(text = it)) },
                label = { Text("متن پیامک (مثلاً LIGHT1_ON)") },
                singleLine = true,
                isError = parts > 1,
                supportingText = {
                    Text(
                        if (parts > 1) "این متن بیش از یک پیامک می‌شود؛ کوتاه‌تر کنید"
                        else "${draft.text.length} نویسه"
                    )
                },
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
