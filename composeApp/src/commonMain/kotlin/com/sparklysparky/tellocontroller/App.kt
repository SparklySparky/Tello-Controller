package com.sparklysparky.tellocontroller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiTethering
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
import com.sparklysparky.tellocontroller.classes.CommandType
import com.sparklysparky.tellocontroller.classes.MainViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with connection status
                ConnectionHeader(
                    isConnected = isConnected,
                    onConnectClick = { viewModel.connect()}
                )

                // Telemetry Dashboard
                TelemetryDashboard(telemetry)

                // Flight Controls Section
                FlightControlsSection(viewModel, isConnected)

                // Movement Controls Section
                MovementControlsSection(viewModel, isConnected)

                // Rotation Controls Section
                RotationControlsSection(viewModel, isConnected)

                // Flip Controls Section
                FlipControlsSection(viewModel, isConnected)

                // Advanced Commands Section
                AdvancedCommandsSection(viewModel, isConnected)

                // Query Commands Section
                QueryCommandsSection(viewModel, isConnected)
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "TELLO DRONE",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Controller v1.0",
                    style = MaterialTheme.typography.bodyMedium,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text(
                        "CONNECTED",
                        style = MaterialTheme.typography.titleMedium,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "TELEMETRY",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Primary gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularGauge(
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

            CircularGauge(
                modifier = Modifier.weight(1f),
                label = "HEIGHT",
                value = telemetry.height,
                maxValue = 500,
                unit = "cm",
                color = Color(0xFF00D4FF)
            )
        }

        // Secondary metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearMetric(
                modifier = Modifier.weight(1f),
                label = "TEMP",
                value = "${telemetry.tempLow}°-${telemetry.tempHigh}°",
                icon = Icons.Default.DeviceThermostat
            )

            LinearMetric(
                modifier = Modifier.weight(1f),
                label = "TIME",
                value = formatFlightTime(telemetry.flightTime),
                icon = Icons.Default.Timer
            )
        }

        // Attitude indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttitudeIndicator(
                modifier = Modifier.weight(1f),
                label = "PITCH",
                value = telemetry.pitch,
                icon = "⬆"
            )
            AttitudeIndicator(
                modifier = Modifier.weight(1f),
                label = "ROLL",
                value = telemetry.roll,
                icon = "↻"
            )
            AttitudeIndicator(
                modifier = Modifier.weight(1f),
                label = "YAW",
                value = telemetry.yaw,
                icon = "⟳"
            )
        }

        // Speed vectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VectorMetric(
                modifier = Modifier.weight(1f),
                label = "VX",
                value = telemetry.vgx
            )
            VectorMetric(
                modifier = Modifier.weight(1f),
                label = "VY",
                value = telemetry.vgy
            )
            VectorMetric(
                modifier = Modifier.weight(1f),
                label = "VZ",
                value = telemetry.vgz
            )
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
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { (value.toFloat() / maxValue).coerceIn(0f, 1f) },
                    modifier = Modifier.size(100.dp),
                    color = color,
                    strokeWidth = 8.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$value",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun LinearMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AttitudeIndicator(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    icon: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                "${value}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun VectorMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: Int
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                "$value",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (value > 0) Color(0xFF4CAF50) else if (value < 0) Color(0xFFEF5350) else Color.White
            )
        }
    }
}

@Composable
fun FlightControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CommandSection(title = "FLIGHT CONTROLS") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "TAKEOFF",
                icon = "🚁",
                color = Color(0xFF4CAF50),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.TAKEOFF) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "LAND",
                icon = "🛬",
                color = Color(0xFF2196F3),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.LAND) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "EMERGENCY",
                icon = "⚠️",
                color = Color(0xFFEF5350),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.EMERGENCY) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "STOP",
                icon = "⏸",
                color = Color(0xFFFFA726),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.STOP) }
            )
        }
    }
}

@Composable
fun MovementControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CommandSection(title = "MOVEMENT") {
        // Vertical controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "UP",
                icon = "⬆️",
                color = Color(0xFF00BCD4),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.UP) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "DOWN",
                icon = "⬇️",
                color = Color(0xFF00BCD4),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.DOWN) }
            )
        }

        // Horizontal controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "LEFT",
                icon = "⬅️",
                color = Color(0xFF9C27B0),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.LEFT) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "RIGHT",
                icon = "➡️",
                color = Color(0xFF9C27B0),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.RIGHT) }
            )
        }

        // Forward/Backward controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "FORWARD",
                icon = "🔼",
                color = Color(0xFFFF9800),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.FORWARD) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "BACKWARD",
                icon = "🔽",
                color = Color(0xFFFF9800),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.BACKWARD) }
            )
        }
    }
}

@Composable
fun RotationControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CommandSection(title = "ROTATION") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "CLOCKWISE",
                icon = "↻",
                color = Color(0xFFE91E63),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.ROTATE_CLOCKWISE) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "C-CLOCKWISE",
                icon = "↺",
                color = Color(0xFFE91E63),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.ROTATE_COUNTERCLOCKWISE) }
            )
        }
    }
}

@Composable
fun FlipControlsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CommandSection(title = "FLIPS") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "FLIP F",
                icon = "🔼",
                color = Color(0xFF673AB7),
                enabled = isConnected,
                compact = true,
                onClick = { viewModel.sendCommand(CommandType.FLIP_FORWARD) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "FLIP B",
                icon = "🔽",
                color = Color(0xFF673AB7),
                enabled = isConnected,
                compact = true,
                onClick = { viewModel.sendCommand(CommandType.FLIP_BACKWARD) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "FLIP L",
                icon = "⬅️",
                color = Color(0xFF673AB7),
                enabled = isConnected,
                compact = true,
                onClick = { viewModel.sendCommand(CommandType.FLIP_LEFT) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "FLIP R",
                icon = "➡️",
                color = Color(0xFF673AB7),
                enabled = isConnected,
                compact = true,
                onClick = { viewModel.sendCommand(CommandType.FLIP_RIGHT) }
            )
        }
    }
}

@Composable
fun AdvancedCommandsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CommandSection(title = "ADVANCED") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "STREAM ON",
                icon = "📹",
                color = Color(0xFF009688),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.STREAM_ON) }
            )

            CommandButton(
                modifier = Modifier.weight(1f),
                label = "STREAM OFF",
                icon = "📷",
                color = Color(0xFF009688),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.STREAM_OFF) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommandButton(
                modifier = Modifier.weight(1f),
                label = "SET SPEED",
                icon = "⚡",
                color = Color(0xFFFFEB3B),
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.SET_SPEED) }
            )
        }
    }
}

@Composable
fun QueryCommandsSection(viewModel: MainViewModel, isConnected: Boolean) {
    CommandSection(title = "QUERIES") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueryButton(
                modifier = Modifier.weight(1f),
                label = "BATTERY",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_BATTERY) }
            )

            QueryButton(
                modifier = Modifier.weight(1f),
                label = "SPEED",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_SPEED) }
            )

            QueryButton(
                modifier = Modifier.weight(1f),
                label = "TIME",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_TIME) }
            )

            QueryButton(
                modifier = Modifier.weight(1f),
                label = "HEIGHT",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_HEIGHT) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueryButton(
                modifier = Modifier.weight(1f),
                label = "TEMP",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_TEMPERATURE) }
            )

            QueryButton(
                modifier = Modifier.weight(1f),
                label = "ACCEL",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_ACCELERATION) }
            )

            QueryButton(
                modifier = Modifier.weight(1f),
                label = "TOF",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_TOF) }
            )

            QueryButton(
                modifier = Modifier.weight(1f),
                label = "BARO",
                enabled = isConnected,
                onClick = { viewModel.sendCommand(CommandType.GET_BAROMETER) }
            )
        }
    }
}

@Composable
fun CommandSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun CommandButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: String,
    color: Color,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(if (compact) 60.dp else 80.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                icon,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall
            )
            if (!compact) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                label,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QueryButton(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// Helper functions
fun formatFlightTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

// Telemetry data class
data class TelemetryData(
    val battery: Int = 0,
    val height: Int = 0,
    val tempLow: Int = 0,
    val tempHigh: Int = 0,
    val flightTime: Int = 0,
    val pitch: Int = 0,
    val roll: Int = 0,
    val yaw: Int = 0,
    val vgx: Int = 0,
    val vgy: Int = 0,
    val vgz: Int = 0
)