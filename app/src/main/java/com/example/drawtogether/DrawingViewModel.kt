package com.example.drawtogether

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DrawingViewModel : ViewModel() {

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes

    private val _currentStroke = MutableStateFlow<Stroke?>(null)
    val currentStroke: StateFlow<Stroke?> = _currentStroke

    private val _selectedColor = MutableStateFlow(0xFF000000L)
    val selectedColor: StateFlow<Long> = _selectedColor

    private val _strokeWidth = MutableStateFlow(8f)
    val strokeWidth: StateFlow<Float> = _strokeWidth

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val wsService = WebSocketService()

    init {
        wsService.onStrokeReceived = { stroke ->
            _strokes.value = _strokes.value + stroke
        }
        wsService.onClearReceived = {
            _strokes.value = emptyList()
        }
        wsService.onConnected = {
            _isConnected.value = true
        }
        wsService.onHistoryReceived = { strokes ->
            _strokes.value = strokes
        }
        wsService.onError = { error ->
            _error.value = error
        }
    }

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun joinRoom(serverUrl: String, roomCode: String) {
        val code = roomCode.uppercase().trim()
        if (code.length != 6) {
            _error.value = "Код должен содержать 6 символов"
            return
        }
        
        _error.value = null
        _roomCode.value = code
        wsService.connect(serverUrl, code)
    }

    fun startStroke(x: Float, y: Float) {
        _currentStroke.value = Stroke(
            points = listOf(Point(x, y)),
            color = _selectedColor.value,
            width = _strokeWidth.value
        )
    }

    fun continueStroke(x: Float, y: Float) {
        _currentStroke.value?.let { current ->
            _currentStroke.value = current.copy(
                points = current.points + Point(x, y)
            )
        }
    }

    fun endStroke() {
        val stroke = _currentStroke.value ?: return
        _currentStroke.value = null
        
        if (stroke.points.size < 2) return
        
        _strokes.value = _strokes.value + stroke
        wsService.sendStroke(stroke)
    }

    fun setColor(color: Long) {
        _selectedColor.value = color
    }

    fun setWidth(width: Float) {
        _strokeWidth.value = width
    }

    fun clearCanvas() {
        _strokes.value = emptyList()
        wsService.sendClear()
    }

    fun leaveRoom() {
        wsService.disconnect()
        _roomCode.value = null
        _isConnected.value = false
        _strokes.value = emptyList()
        _currentStroke.value = null
    }

    override fun onCleared() {
        super.onCleared()
        wsService.disconnect()
    }
}
