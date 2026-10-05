@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.drawtogether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val vm: DrawingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                DrawTogetherApp(vm)
            }
        }
    }
}

@Composable
fun DrawTogetherApp(vm: DrawingViewModel) {
    val isConnected by vm.isConnected.collectAsState()

    AnimatedContent(
        targetState = isConnected,
        label = "screen_transition"
    ) { connected ->
        if (!connected) {
            LobbyScreen(vm)
        } else {
            DrawingScreen(vm)
        }
    }
}

@Composable
fun LobbyScreen(vm: DrawingViewModel) {
    var serverUrl by remember { mutableStateOf("wss://draw-together.onrender.com") }
    var roomCode by remember { mutableStateOf("") }
    val error by vm.error.collectAsState()

    Box(
        Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF6C5CE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Brush,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Draw Together",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Рисуй с друзьями онлайн",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("URL сервера", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF6C5CE7),
                    focusedBorderColor = Color(0xFF6C5CE7),
                    unfocusedBorderColor = Color(0xFF3A3F5C)
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { 
                        if (it.length <= 6) roomCode = it.uppercase()
                    },
                    label = { Text("Код комнаты", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6C5CE7),
                        focusedBorderColor = Color(0xFF6C5CE7),
                        unfocusedBorderColor = Color(0xFF3A3F5C)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = { roomCode = vm.generateRoomCode() },
                    modifier = Modifier.size(56.dp).background(Color(0xFF6C5CE7), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Generate", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.joinRoom(serverUrl, roomCode) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                shape = RoundedCornerShape(16.dp),
                enabled = roomCode.length == 6
            ) {
                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("ВОЙТИ В КОМНАТУ", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE74C3C).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        error!!,
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("💡 Как использовать:", color = Color(0xFF6C5CE7), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("1. Разверни сервер на Render (один раз)", color = Color.Gray, fontSize = 12.sp)
                    Text("2. Введи URL сервера (wss://...)", color = Color.Gray, fontSize = 12.sp)
                    Text("3. Создай код комнаты или введи код друга", color = Color.Gray, fontSize = 12.sp)
                    Text("4. Жми 'ВОЙТИ В КОМНАТУ' и рисуй!", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DrawingScreen(vm: DrawingViewModel) {
    val strokes by vm.strokes.collectAsState()
    val currentStroke by vm.currentStroke.collectAsState()
    val selectedColor by vm.selectedColor.collectAsState()
    val strokeWidth by vm.strokeWidth.collectAsState()
    val roomCode by vm.roomCode.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }

    val colors = listOf(
        0xFF000000L, 0xFFE74C3CL, 0xFFE67E22L, 0xFFF1C40FL,
        0xFF2ECC71L, 0xFF3498DBL, 0xFF9B59B6L, 0xFFECF0F1L
    )

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF2A2A3E)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.leaveRoom() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Column(Modifier.weight(1f)) {
                Text("Комната", color = Color.Gray, fontSize = 12.sp)
                Text(
                    roomCode ?: "",
                    color = Color(0xFF6C5CE7),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            }

            IconButton(onClick = { vm.clearCanvas() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFE74C3C))
            }
        }

        DrawingCanvas(
            strokes = strokes,
            currentStroke = currentStroke,
            onStrokeStart = vm::startStroke,
            onStrokeMove = vm::continueStroke,
            onStrokeEnd = vm::endStroke,
            modifier = Modifier.weight(1f)
        )

        Row(
            Modifier.fillMaxWidth().background(Color(0xFF2A2A3E)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier.size(48.dp)
                    .clip(CircleShape)
                    .background(Color(selectedColor))
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { showColorPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            colors.take(5).forEach { color ->
                Box(
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (selectedColor == color) 3.dp else 1.dp,
                            color = if (selectedColor == color) Color.White else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { vm.setColor(color) }
                )
            }

            Spacer(Modifier.weight(1f))

            Text("${strokeWidth.toInt()}", color = Color.White, fontSize = 14.sp)
            Slider(
                value = strokeWidth,
                onValueChange = { vm.setWidth(it) },
                valueRange = 2f..30f,
                modifier = Modifier.width(100.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6C5CE7),
                    activeTrackColor = Color(0xFF6C5CE7)
                )
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = selectedColor,
            onColorSelected = { 
                vm.setColor(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val allColors = listOf(
        0xFF000000L, 0xFF434343L, 0xFF666666L, 0xFF999999L, 0xFFCCCCCC, 0xFFEFEBE9L, 0xFFFFFFFFL,
        0xFFE74C3CL, 0xFFE67E22L, 0xFFF1C40FL, 0xFF2ECC71L, 0xFF1ABC9CL, 0xFF3498DBL, 0xFF9B59B6L,
        0xFFC0392BL, 0xFFD35400L, 0xFFF39C12L, 0xFF27AE60L, 0xFF16A085L, 0xFF2980B9L, 0xFF8E44ADL
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбери цвет", color = Color.White) },
        text = {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(currentColor))
                )
                Spacer(Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allColors.chunked(7).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { color ->
                                Box(
                                    Modifier.size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            width = if (color == currentColor) 3.dp else 1.dp,
                                            color = if (color == currentColor) Color.White else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { onColorSelected(color) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = Color(0xFF6C5CE7))
            }
        },
        containerColor = Color(0xFF2A2A3E),
        shape = RoundedCornerShape(20.dp)
    )
}
