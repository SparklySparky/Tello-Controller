package com.sparklysparky.tellocontroller

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparklysparky.tellocontroller.classes.CommandType
import com.sparklysparky.tellocontroller.classes.MainViewModel

@Composable
fun App() {
    val viewModel = remember { MainViewModel() }
    val isConnected by viewModel.isConnected.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00D4FF),
            secondary = Color(0xFFFF6B00),
            background = Color(0xFF0A0E27),
            surface = Color(0xFF1A1F3A),
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E27),
                            Color(0xFF1A1F3A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Header
                ConnectionHeader(
                    isConnected = isConnected,
                    onConnectClick = { viewModel.connect() }
                )

                // Main Content: 3 Column Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT COLUMN: Telemetry
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryDashboard(telemetry)
                    }

                    // MIDDLE COLUMN: Video (Scalable)
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        VideoStreamDisplay(viewModel, isConnected)
                    }

                    // RIGHT COLUMN: Controls
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlightControlsSection(viewModel, isConnected)
                        MovementControlsSection(viewModel, isConnected)
                        RotationControlsSection(viewModel, isConnected)
                        FlipControlsSection(viewModel, isConnected)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionHeader(isConnected: Boolean, onConnectClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF1B5E20) else Color(0xFF8B0000)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "🚁 TELLO DRONE CONTROLLER",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
                Text(
                    "v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            if (!isConnected) {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CONNETTITI", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text(
                        "CONNESSO",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun VideoStreamDisplay(viewModel: MainViewModel, isConnected: Boolean) {
    val isVideoStreaming by viewModel.isVideoStreaming.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📹 VIDEO LIVE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )

                Button(
                    onClick = { viewModel.toggleVideoStream() },
                    enabled = isConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVideoStreaming) Color(0xFFEF5350) else Color(0xFF009688)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isVideoStreaming) "⏹ FERMA" else "▶ AVVIA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Video Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentFrame != null) {
                        Image(
                            bitmap = currentFrame!!,
                            contentDescription = "Tello Live Feed",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // LIVE indicator overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        Color.Red.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    "LIVE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                if (isVideoStreaming) "Decodifica..." else "Stream non attivo",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            if (!isConnected) {
                                Text(
                                    "Connettiti al drone",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryDashboard(telemetry: TelemetryData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "📊 TELEMETRIA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Row 1: Battery, Height, TOF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularGauge(
                    modifier = Modifier.weight(1f),
                    label = "BATTERIA",
                    value = telemetry.battery,
                    maxValue = 100,
                    unit = "%",
                    color = when {
                        telemetry.battery > 50 -> Color(0xFF4CAF50)
                        telemetry.battery > 20 -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    }
                )

                CircularGauge(
                    modifier = Modifier.weight(1f),
                    label = "ALTEZZA",
                    value = telemetry.height,
                    maxValue = 500,
                    unit = "cm",
                    color = Color(0xFF00D4FF)
                )

                CircularGauge(
                    modifier = Modifier.weight(1f),
                    label = "TOF",
                    value = telemetry.tof,
                    maxValue = 800,
                    unit = "cm",
                    color = Color(0xFF9C27B0)
                )
            }

            // Row 2: Temps & Flight Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularGauge(
                    modifier = Modifier.weight(1f),
                    label = "TEMP MIN",
                    value = telemetry.tempLow,
                    maxValue = 150,
                    unit = "°C",
                    color = Color(0xFFFF9800)
                )

                CircularGauge(
                    modifier = Modifier.weight(1f),
                    label = "TEMP MAX",
                    value = telemetry.tempHigh,
                    maxValue = 150,
                    unit = "°C",
                    color = Color(0xFFFF5722)
                )

                CircularGauge(
                    modifier = Modifier.weight(1f),
                    label = "VOLO",
                    value = telemetry.flightTime,
                    maxValue = 900,
                    unit = "",
                    color = Color(0xFF2196F3),
                    displayValue = formatTime(telemetry.flightTime)
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.1f))

            // Attitude
            Text(
                "Orientamento",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Rotazione X",
                    value = telemetry.roll,
                    minValue = -180,
                    maxValue = 180,
                    unit = "°",
                    color = Color(0xFF00BCD4)
                )

                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Rotazione Y",
                    value = telemetry.pitch,
                    minValue = -180,
                    maxValue = 180,
                    unit = "°",
                    color = Color(0xFF00BCD4)
                )

                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Rotazione Z",
                    value = telemetry.yaw,
                    minValue = -180,
                    maxValue = 180,
                    unit = "°",
                    color = Color(0xFF00BCD4)
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.1f))

            // Velocity
            Text(
                "Velocità",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Vx",
                    value = telemetry.vgx,
                    minValue = -100,
                    maxValue = 100,
                    unit = "cm/s",
                    color = Color(0xFFE91E63)
                )

                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Vy",
                    value = telemetry.vgy,
                    minValue = -100,
                    maxValue = 100,
                    unit = "cm/s",
                    color = Color(0xFFE91E63)
                )

                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Vz",
                    value = telemetry.vgz,
                    minValue = -100,
                    maxValue = 100,
                    unit = "cm/s",
                    color = Color(0xFFE91E63)
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.1f))

            Text(
                "Accellerazione",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Ax",
                    value = (telemetry.agx).toInt(),
                    minValue = -2000,
                    maxValue = 2000,
                    unit = "g",
                    color = Color(0xFF4CAF50),
                    displayValue = String.format("%.2f", telemetry.agx)
                )

                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Ay",
                    value = (telemetry.agy).toInt(),
                    minValue = -2000,
                    maxValue = 2000,
                    unit = "g",
                    color = Color(0xFF4CAF50),
                    displayValue = String.format("%.2f", telemetry.agy)
                )

                CircularGaugeWithCenter(
                    modifier = Modifier.weight(1f),
                    label = "Az",
                    value = telemetry.agz.toInt(),
                    minValue = -2000,
                    maxValue = 2000,
                    unit = "g",
                    color = Color(0xFF4CAF50),
                    displayValue = if(telemetry.agz == 0f) { String.format("%.2f", telemetry.agz) } else { String.format("%.2f", telemetry.agz + 1000) }
                )
            }
        }
    }
}

@Composable
fun CircularGauge(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    maxValue: Int,
    unit: String,
    color: Color,
    displayValue: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (value.toFloat() / maxValue).coerceIn(0f, 1f) },
                    modifier = Modifier.size(60.dp),
                    color = color,
                    strokeWidth = 5.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        displayValue ?: "$value",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 14.sp
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CircularGaugeWithCenter(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    unit: String,
    color: Color,
    displayValue: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(60.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    strokeWidth = 5.dp
                )

                val normalizedValue = ((value - minValue).toFloat() / (maxValue - minValue)).coerceIn(0f, 1f)
                CircularProgressIndicator(
                    progress = { normalizedValue },
                    modifier = Modifier.size(60.dp),
                    color = color,
                    strokeWidth = 5.dp,
                    trackColor = Color.Transparent
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        displayValue ?: "$value",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 14.sp
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IncrementSelector(
    label: String,
    currentValue: Int,
    options: List<Int>,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { value ->
                val isSelected = value == currentValue
                Button(
                    onClick = { onValueChange(value) },
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text(
                        "$value$unit",
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FlightControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CompactCommandSection("🚁 VOLO") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "DECOLLO", "🚁", Color(0xFF4CAF50), isConnected) {
                viewModel.sendCommand(CommandType.TAKEOFF, "")
            }
            CompactButton(Modifier.weight(1f), "ATTERRA", "🛬", Color(0xFF2196F3), isConnected) {
                viewModel.sendCommand(CommandType.LAND, "")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "STOP", "⏸", Color(0xFFFFA726), isConnected) {
                viewModel.sendCommand(CommandType.STOP, "")
            }
            CompactButton(Modifier.weight(1f), "EMRG", "⚠️", Color(0xFFEF5350), isConnected) {
                viewModel.sendCommand(CommandType.EMERGENCY, "")
            }
        }
    }
}

@Composable
fun MovementControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    val movementIncrement by viewModel.movementIncrement.collectAsState()

    CompactCommandSection("🎮 MOVIMENTO") {
        IncrementSelector(
            label = "Distanza:",
            currentValue = movementIncrement,
            options = listOf(20, 50, 100, 250, 500),
            unit = "cm",
            onValueChange = { viewModel.setMovementIncrement(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "SU", "⬆️", Color(0xFF00BCD4), isConnected) {
                viewModel.sendCommand(CommandType.UP, movementIncrement.toString())
            }
            CompactButton(Modifier.weight(1f), "GIÙ", "⬇️", Color(0xFF00BCD4), isConnected) {
                viewModel.sendCommand(CommandType.DOWN, movementIncrement.toString())
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "SX", "⬅️", Color(0xFF9C27B0), isConnected) {
                viewModel.sendCommand(CommandType.LEFT, movementIncrement.toString())
            }
            CompactButton(Modifier.weight(1f), "DX", "➡️", Color(0xFF9C27B0), isConnected) {
                viewModel.sendCommand(CommandType.RIGHT, movementIncrement.toString())
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "AVANTI", "🔼", Color(0xFFFF9800), isConnected) {
                viewModel.sendCommand(CommandType.FORWARD, movementIncrement.toString())
            }
            CompactButton(Modifier.weight(1f), "INDIETRO", "🔽", Color(0xFFFF9800), isConnected) {
                viewModel.sendCommand(CommandType.BACKWARD, movementIncrement.toString())
            }
        }
    }
}

@Composable
fun RotationControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    val rotationIncrement by viewModel.rotationIncrement.collectAsState()

    CompactCommandSection("🔄 ROTAZIONE") {
        IncrementSelector(
            label = "Angolo:",
            currentValue = rotationIncrement,
            options = listOf(15, 45, 90, 180, 360),
            unit = "°",
            onValueChange = { viewModel.setRotationIncrement(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "SINISTRA", "⬅️", Color(0xFFE91E63), isConnected) {
                viewModel.sendCommand(CommandType.ROTATE_COUNTERCLOCKWISE, rotationIncrement.toString())
            }

            CompactButton(Modifier.weight(1f), "DESTRA", "➡️", Color(0xFFE91E63), isConnected) {
                viewModel.sendCommand(CommandType.ROTATE_CLOCKWISE, rotationIncrement.toString())
            }
        }
    }
}

@Composable
fun FlipControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CompactCommandSection("🤸 CAPRIOLA") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "AV", "🔼", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_FORWARD, "")
            }
            CompactButton(Modifier.weight(1f), "IN", "🔽", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_BACKWARD, "")
            }
            CompactButton(Modifier.weight(1f), "SX", "⬅️", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_LEFT, "")
            }
            CompactButton(Modifier.weight(1f), "DX", "➡️", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_RIGHT, "")
            }
        }
    }
}

@Composable
fun CompactCommandSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp
            )
            content()
        }
    }
}

@Composable
fun CompactButton(
    modifier: Modifier,
    label: String,
    icon: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

data class TelemetryData(
    val pitch: Int = 0,
    val roll: Int = 0,
    val yaw: Int = 0,
    val vgx: Int = 0,
    val vgy: Int = 0,
    val vgz: Int = 0,
    val tempLow: Int = 0,
    val tempHigh: Int = 0,
    val tof: Int = 0,
    val height: Int = 0,
    val battery: Int = 0,
    val baro: Float = 0f,
    val flightTime: Int = 0,
    val agx: Float = 0f,
    val agy: Float = 0f,
    val agz: Float = 0f
)