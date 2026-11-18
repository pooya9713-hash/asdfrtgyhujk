package com.homecontrol.app.ui.devices

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homecontrol.app.data.CategoryEntity
import com.homecontrol.app.data.DeviceEntity
import com.homecontrol.app.data.DeviceWithCommands
import com.homecontrol.app.ui.components.DeviceIcons
import com.homecontrol.app.ui.components.EmptyState
import com.homecontrol.app.ui.components.ScreenHeader
import com.homecontrol.app.ui.components.ltr

private data class CategoryDialogState(val category: CategoryEntity?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevicesScreen(
    vm: DevicesViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<DeviceEntity?>(null) }
    var categoryDialog by remember { mutableStateOf<CategoryDialogState?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader("مدیریت وسایل", "ساخت، ویرایش و مرتب‌سازی دکمه‌های کنترلی")
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("دسته‌بندی‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "برای تغییر نام یا حذف، روی دسته بزنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.categories.forEach { cat ->
                                AssistChip(
                                    onClick = { categoryDialog = CategoryDialogState(cat) },
                                    label = { Text(cat.name) }
                                )
                            }
                            AssistChip(
                                onClick = { categoryDialog = CategoryDialogState(null) },
                                label = { Text("دسته جدید") },
                                leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }

            if (state.devices.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Dashboard,
                        title = "وسیله‌ای وجود ندارد",
                        subtitle = "یک وسیله جدید بسازید یا چند نمونه آماده اضافه کنید تا با ساختار برنامه آشنا شوید."
                    ) {
                        Button(onClick = onAdd) { Text("افزودن وسیله") }
                        Spacer(Modifier.size(8.dp))
                        OutlinedButton(onClick = vm::addSamples) { Text("افزودن چند نمونه") }
                    }
                }
            }

            itemsIndexed(state.devices, key = { _, d -> d.device.id }) { index, item ->
                DeviceRow(
                    item = item,
                    categoryName = state.categories.firstOrNull { it.id == item.device.categoryId }?.name,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.devices.lastIndex,
                    onMoveUp = { vm.move(item.device.id, true) },
                    onMoveDown = { vm.move(item.device.id, false) },
                    onEdit = { onEdit(item.device.id) },
                    onDelete = { deleteTarget = item.device }
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("وسیله جدید") }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف وسیله") },
            text = { Text("«${target.name}» و همه فرمان‌های آن حذف می‌شود. این کار قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteDevice(target.id); deleteTarget = null }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } }
        )
    }

    categoryDialog?.let { dialog ->
        CategoryDialog(
            category = dialog.category,
            onSave = { name -> vm.saveCategory(dialog.category?.id ?: 0L, name); categoryDialog = null },
            onDelete = dialog.category?.let { cat -> { vm.deleteCategory(cat.id); categoryDialog = null } },
            onDismiss = { categoryDialog = null }
        )
    }
}

@Composable
private fun CategoryDialog(
    category: CategoryEntity?,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "دسته جدید" else "ویرایش دسته") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته") },
                    singleLine = true
                )
                if (onDelete != null) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "با حذف دسته، وسایل آن حذف نمی‌شوند و فقط بدون دسته می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("ذخیره") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}

@Composable
private fun DeviceRow(
    item: DeviceWithCommands,
    categoryName: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val device = item.device
    val accent = Color(device.colorHex)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(accent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(DeviceIcons.get(device.iconKey), null, tint = accent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val parts = buildList {
                        add("${item.commands.size} فرمان")
                        if (categoryName != null) add(categoryName)
                        add(if (device.destNumber.isNullOrBlank()) "شماره اصلی" else ltr(device.destNumber))
                    }
                    Text(
                        parts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "بالا")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "پایین")
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "ویرایش") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
