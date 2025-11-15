package com.sparklysparky.tellocontroller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

                // Main Content: Split Screen
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Side: Telemetry
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryDashboard(telemetry)
                    }

                    // Right Side: Controls
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
                        AdvancedCommandsSection(viewModel, isConnected)
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
        shape = RoundedCornerShape(12.dp)
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
                    "TELLO DRONE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Controller v1.0",
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
                    Text("CONNECT", fontWeight = FontWeight.Bold)
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
                        "CONNECTED",
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
fun TelemetryDashboard(telemetry: TelemetryData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "TELEMETRY",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Battery & Height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactGauge(
                    modifier = Modifier.weight(1f),
                    label = "BATTERY",
                    value = telemetry.battery,
                    maxValue = 100,
                    unit = "%",
                    color = when {
                        telemetry.battery > 50 -> Color(0xFF4CAF50)
                        telemetry.battery > 20 -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    }
                )

                CompactGauge(
                    modifier = Modifier.weight(1f),
                    label = "HEIGHT",
                    value = telemetry.height,
                    maxValue = 500,
                    unit = "cm",
                    color = Color(0xFF00D4FF)
                )
            }

            // Metrics Grid
            CompactMetric("TEMP", "${telemetry.tempLow}°-${telemetry.tempHigh}°C", Icons.Default.DeviceThermostat)
            CompactMetric("TIME", formatTime(telemetry.flightTime), Icons.Default.Timer)
            CompactMetric("TOF", "${telemetry.tof} cm", Icons.Default.Straighten)
            CompactMetric("BARO", String.format("%.2f cm", telemetry.baro), Icons.Default.Compress)

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Attitude
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniMetric(Modifier.weight(1f), "PITCH", "${telemetry.pitch}°")
                MiniMetric(Modifier.weight(1f), "ROLL", "${telemetry.roll}°")
                MiniMetric(Modifier.weight(1f), "YAW", "${telemetry.yaw}°")
            }

            // Velocity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniMetric(Modifier.weight(1f), "VX", "${telemetry.vgx}")
                MiniMetric(Modifier.weight(1f), "VY", "${telemetry.vgy}")
                MiniMetric(Modifier.weight(1f), "VZ", "${telemetry.vgz}")
            }

            // Acceleration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniMetric(Modifier.weight(1f), "AX", String.format("%.2f", telemetry.agx))
                MiniMetric(Modifier.weight(1f), "AY", String.format("%.2f", telemetry.agy))
                MiniMetric(Modifier.weight(1f), "AZ", String.format("%.2f", telemetry.agz))
            }
        }
    }
}

@Composable
fun CompactGauge(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    maxValue: Int,
    unit: String,
    color: Color
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
                        "$value",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 16.sp
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
fun CompactMetric(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}

@Composable
fun MiniMetric(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FlightControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CompactCommandSection("FLIGHT") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "TAKEOFF", "🚁", Color(0xFF4CAF50), isConnected) {
                viewModel.sendCommand(CommandType.TAKEOFF)
            }
            CompactButton(Modifier.weight(1f), "LAND", "🛬", Color(0xFF2196F3), isConnected) {
                viewModel.sendCommand(CommandType.LAND)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "STOP", "⏸", Color(0xFFFFA726), isConnected) {
                viewModel.sendCommand(CommandType.STOP)
            }
            CompactButton(Modifier.weight(1f), "EMRG", "⚠️", Color(0xFFEF5350), isConnected) {
                viewModel.sendCommand(CommandType.EMERGENCY)
            }
        }
    }
}

@Composable
fun MovementControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CompactCommandSection("MOVEMENT") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "UP", "⬆️", Color(0xFF00BCD4), isConnected) {
                viewModel.sendCommand(CommandType.UP)
            }
            CompactButton(Modifier.weight(1f), "DOWN", "⬇️", Color(0xFF00BCD4), isConnected) {
                viewModel.sendCommand(CommandType.DOWN)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "LEFT", "⬅️", Color(0xFF9C27B0), isConnected) {
                viewModel.sendCommand(CommandType.LEFT)
            }
            CompactButton(Modifier.weight(1f), "RIGHT", "➡️", Color(0xFF9C27B0), isConnected) {
                viewModel.sendCommand(CommandType.RIGHT)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "FWD", "🔼", Color(0xFFFF9800), isConnected) {
                viewModel.sendCommand(CommandType.FORWARD)
            }
            CompactButton(Modifier.weight(1f), "BACK", "🔽", Color(0xFFFF9800), isConnected) {
                viewModel.sendCommand(CommandType.BACKWARD)
            }
        }
    }
}

@Composable
fun RotationControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CompactCommandSection("ROTATION") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "CW", "↻", Color(0xFFE91E63), isConnected) {
                viewModel.sendCommand(CommandType.ROTATE_CLOCKWISE)
            }
            CompactButton(Modifier.weight(1f), "CCW", "↺", Color(0xFFE91E63), isConnected) {
                viewModel.sendCommand(CommandType.ROTATE_COUNTERCLOCKWISE)
            }
        }
    }
}

@Composable
fun FlipControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CompactCommandSection("FLIPS") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "F", "🔼", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_FORWARD)
            }
            CompactButton(Modifier.weight(1f), "B", "🔽", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_BACKWARD)
            }
            CompactButton(Modifier.weight(1f), "L", "⬅️", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_LEFT)
            }
            CompactButton(Modifier.weight(1f), "R", "➡️", Color(0xFF673AB7), isConnected) {
                viewModel.sendCommand(CommandType.FLIP_RIGHT)
            }
        }
    }
}

@Composable
fun AdvancedCommandsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CompactCommandSection("ADVANCED") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactButton(Modifier.weight(1f), "STREAM", "📹", Color(0xFF009688), isConnected) {
                viewModel.sendCommand(CommandType.STREAM_ON)
            }
            CompactButton(Modifier.weight(1f), "SPEED", "⚡", Color(0xFFFFEB3B), isConnected) {
                viewModel.sendCommand(CommandType.SET_SPEED)
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
        shape = RoundedCornerShape(12.dp)
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
                fontSize = 12.sp
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

// Helper function
fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

// Telemetry data class
data class TelemetryData(
    val pitch: Int = 0,
    val roll: Int = 0,
    val yaw: Int = 0,
    val vgx: Int = 0,           // velocity X
    val vgy: Int = 0,           // velocity Y
    val vgz: Int = 0,           // velocity Z
    val tempLow: Int = 0,       // lowest temp
    val tempHigh: Int = 0,      // highest temp
    val tof: Int = 0,           // time of flight distance
    val height: Int = 0,        // h
    val battery: Int = 0,       // bat
    val baro: Float = 0f,       // barometer
    val flightTime: Int = 0,    // motor time in seconds
    val agx: Float = 0f,        // acceleration X
    val agy: Float = 0f,        // acceleration Y
    val agz: Float = 0f         // acceleration Z
)