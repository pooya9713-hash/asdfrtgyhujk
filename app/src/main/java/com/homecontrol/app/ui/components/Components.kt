package com.homecontrol.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

/** Keeps phone numbers / latin text left-to-right inside RTL text. */
fun ltr(text: String): String = "\u202A$text\u202C"

data class DeviceIcon(val key: String, val label: String, val vector: ImageVector)

object DeviceIcons {
    val all = listOf(
        DeviceIcon("light", "چراغ", Icons.Filled.Lightbulb),
        DeviceIcon("ac", "کولر", Icons.Filled.AcUnit),
        DeviceIcon("heater", "بخاری", Icons.Filled.LocalFireDepartment),
        DeviceIcon("thermostat", "ترموستات", Icons.Filled.Thermostat),
        DeviceIcon("fan", "پنکه", Icons.Filled.Air),
        DeviceIcon("door", "درب", Icons.Filled.DoorFront),
        DeviceIcon("garage", "پارکینگ", Icons.Filled.Garage),
        DeviceIcon("window", "پنجره", Icons.Filled.Window),
        DeviceIcon("lock", "قفل", Icons.Filled.Lock),
        DeviceIcon("alarm", "دزدگیر", Icons.Filled.Security),
        DeviceIcon("camera", "دوربین", Icons.Filled.Videocam),
        DeviceIcon("tv", "تلویزیون", Icons.Filled.Tv),
        DeviceIcon("speaker", "پخش صدا", Icons.Filled.Speaker),
        DeviceIcon("water", "پمپ آب", Icons.Filled.WaterDrop),
        DeviceIcon("kitchen", "آشپزخانه", Icons.Filled.Kitchen),
        DeviceIcon("router", "مودم", Icons.Filled.Router),
        DeviceIcon("power", "پریز / برق", Icons.Filled.Power),
        DeviceIcon("bolt", "برق", Icons.Filled.Bolt),
        DeviceIcon("home", "سایر", Icons.Filled.Home)
    )

    fun get(key: String): ImageVector = all.firstOrNull { it.key == key }?.vector ?: Icons.Filled.Power
}

val DeviceColors: List<Long> = listOf(
    0xFF6C8CFF, 0xFF38E1C6, 0xFFFFB84D, 0xFFFF7AC6,
    0xFFFF6B6B, 0xFF9B6CFF, 0xFF4CD964, 0xFF40C4FF
)

/** A button with a springy press animation, ripple and a custom background brush. */
@Composable
fun PressableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    background: Brush,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "press-scale"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(interactionSource = source, indication = ripple(), onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        actions()
    }
}
