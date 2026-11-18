package com.homecontrol.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homecontrol.app.data.DeliveryStatus
import com.homecontrol.app.data.HistoryEntity
import com.homecontrol.app.data.SendStatus
import com.homecontrol.app.ui.components.EmptyState
import com.homecontrol.app.ui.components.Pill
import com.homecontrol.app.ui.components.ScreenHeader
import com.homecontrol.app.ui.components.ltr
import com.homecontrol.app.ui.theme.AppColors
import com.homecontrol.app.util.DateFormatter

@Composable
fun HistoryScreen(vm: HistoryViewModel) {
    val items by vm.items.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader("تاریخچه فرمان‌ها", "${items.size} مورد") {
                if (items.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "پاک کردن همه")
                    }
                }
            }
        }
        if (items.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.History,
                    title = "تاریخچه خالی است",
                    subtitle = "پس از ارسال هر فرمان، وضعیت آن اینجا ثبت می‌شود."
                )
            }
        }
        items(items, key = { it.id }) { h ->
            HistoryItem(h, onDelete = { vm.delete(h.id) })
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("پاک کردن تاریخچه") },
            text = { Text("همه موارد تاریخچه حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = { vm.clear(); confirmClear = false }) {
                    Text("پاک کن", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun HistoryItem(h: HistoryEntity, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (statusText, statusColor) = when (h.status) {
        SendStatus.SENT -> "ارسال شد" to AppColors.success
        SendStatus.FAILED -> "ناموفق" to scheme.error
        else -> "در حال ارسال" to AppColors.warning
    }
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${h.deviceName} • ${h.commandLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Pill(statusText, statusColor)
            }
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = scheme.surfaceVariant) {
                Text(
                    h.smsText,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "به: ${ltr(h.number)}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                DateFormatter.format(h.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            if (!h.error.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(h.error, style = MaterialTheme.typography.bodySmall, color = scheme.error)
            }
            // Delivery is only shown when a delivery report was actually received from the network.
            when (h.delivery) {
                DeliveryStatus.DELIVERED -> {
                    Spacer(Modifier.height(6.dp))
                    Pill("تحویل داده شد (گزارش شبکه)", AppColors.success)
                }
                DeliveryStatus.DELIVERY_FAILED -> {
                    Spacer(Modifier.height(6.dp))
                    Pill("تحویل ناموفق (گزارش شبکه)", scheme.error)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = scheme.onSurfaceVariant)
                }
            }
        }
    }
}
