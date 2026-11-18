package com.homecontrol.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homecontrol.app.data.ThemeMode
import com.homecontrol.app.ui.components.ScreenHeader
import com.homecontrol.app.ui.components.ltr
import com.homecontrol.app.util.Countries

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    snackbar: SnackbarHostState,
    onOpenHistory: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var phoneInput by remember(settings.phone) { mutableStateOf(settings.phone) }
    var countryCode by remember(settings.countryCode) { mutableStateOf(settings.countryCode) }
    var phoneError by remember { mutableStateOf(false) }
    var countryMenu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportBackup(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importBackup(uri) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader("تنظیمات", "شماره مقصد، ظاهر و پشتیبان‌گیری")

        SettingsCard("شماره تلفن مقصد") {
            Text(
                "فرمان‌ها به این شماره پیامک می‌شوند (مگر اینکه برای یک وسیله شماره اختصاصی بگذارید).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Box {
                OutlinedButton(onClick = { countryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    val name = Countries.all.firstOrNull { it.code == countryCode }?.name ?: "کشور"
                    Text("$name (${ltr(countryCode)})", modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, null)
                }
                DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }) {
                    Countries.all.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.name} (${ltr(country.code)})") },
                            onClick = { countryCode = country.code; countryMenu = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it; phoneError = false },
                label = { Text("شماره تلفن (مثال: 09121234567)") },
                singleLine = true,
                isError = phoneError,
                supportingText = { if (phoneError) Text("شماره نامعتبر است") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.savePhone(phoneInput, countryCode) { ok -> phoneError = !ok } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره شماره") }
            if (settings.phone.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "شماره ذخیره‌شده: ${ltr(settings.phone)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        SettingsCard("ظاهر برنامه") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.DARK to "تیره",
                    ThemeMode.LIGHT to "روشن",
                    ThemeMode.SYSTEM to "مطابق سیستم"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.theme == mode,
                        onClick = { vm.setTheme(mode) },
                        label = { Text(label) }
                    )
                }
            }
        }

        SettingsCard("بازخورد دکمه‌ها") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("صدای کلیک", fontWeight = FontWeight.SemiBold)
                    Text(
                        "هنگام فشردن دکمه‌های کنترلی پخش می‌شود (به تنظیمات صدای گوشی بستگی دارد).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = settings.sound, onCheckedChange = vm::setSound)
            }
        }

        SettingsCard("پیامک‌ها و تاریخچه") {
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text("مدیریت پیامک‌های ارسالی / نمایش تاریخچه")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Text("پاک کردن تاریخچه", color = MaterialTheme.colorScheme.error)
            }
        }

        SettingsCard("پشتیبان‌گیری") {
            Text(
                "وسایل، فرمان‌ها، دسته‌ها و تنظیمات در یک فایل JSON ذخیره می‌شوند. تاریخچه در پشتیبان نیست.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportLauncher.launch("homecontrol-backup.json") },
                    modifier = Modifier.weight(1f)
                ) { Text("ذخیره پشتیبان") }
                OutlinedButton(
                    onClick = { confirmImport = true },
                    modifier = Modifier.weight(1f)
                ) { Text("بازیابی") }
            }
        }

        Text(
            "HomeControl نسخه 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("پاک کردن تاریخچه") },
            text = { Text("همه موارد تاریخچه حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) {
                    Text("پاک کن", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("انصراف") } }
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("بازیابی از پشتیبان") },
            text = { Text("وسایل، دسته‌ها و تنظیمات فعلی با محتوای فایل جایگزین می‌شود. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("انتخاب فایل") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
