package com.example.drawtogether

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebSocketService {
    private var webSocket: WebSocketClient? = null
    private val gson = Gson()
    
    var onStrokeReceived: ((Stroke) -> Unit)? = null
    var onClearReceived: (() -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onHistoryReceived: ((List<Stroke>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    fun connect(serverUrl: String, roomCode: String) {
        try {
            val wsUrl = when {
                serverUrl.startsWith("wss://") || serverUrl.startsWith("ws://") -> serverUrl
                serverUrl.startsWith("https://") -> serverUrl.replace("https://", "wss://")
                serverUrl.startsWith("http://") -> serverUrl.replace("http://", "ws://")
                else -> "wss://$serverUrl"
            }
            
            Log.d("WS", "Connecting to: $wsUrl")
            
            webSocket = object : WebSocketClient(URI(wsUrl)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    Log.d("WS", "Connected successfully")
                    val joinMsg = mapOf(
                        "type" to "join",
                        "roomCode" to roomCode
                    )
                    send(gson.toJson(joinMsg))
                    onConnected?.invoke()
                }
                
                override fun onMessage(message: String?) {
                    try {
                        Log.d("WS", "Received: $message")
                        val json = JsonParser.parseString(message).asJsonObject
                        val type = json.get("type").asString
                        
                        when (type) {
                            "stroke" -> {
                                val strokeJson = json.getAsJsonObject("stroke")
                                val stroke = gson.fromJson(strokeJson, Stroke::class.java)
                                onStrokeReceived?.invoke(stroke)
                            }
                            "clear" -> {
                                onClearReceived?.invoke()
                            }
                            "history" -> {
                                val strokesArray = json.getAsJsonArray("strokes")
                                val strokes = mutableListOf<Stroke>()
                                strokesArray.forEach { element ->
                                    strokes.add(gson.fromJson(element, Stroke::class.java))
                                }
                                onHistoryReceived?.invoke(strokes)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WS", "Error parsing message", e)
                    }
                }
                
                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Log.d("WS", "Closed: $reason")
                    onError?.invoke("Соединение закрыто")
                }
                
                override fun onError(ex: Exception?) {
                    Log.e("WS", "Error", ex)
                    onError?.invoke("Ошибка: ${ex?.message ?: "неизвестная"}")
                }
            }
            
            webSocket?.connect()
        } catch (e: Exception) {
            Log.e("WS", "Connection error", e)
            onError?.invoke("Не удалось подключиться: ${e.message}")
        }
    }
    
    fun sendStroke(stroke: Stroke) {
        val message = mapOf(
            "type" to "stroke",
            "stroke" to stroke
        )
        webSocket?.send(gson.toJson(message))
    }
    
    fun sendClear() {
        val message = mapOf("type" to "clear")
        webSocket?.send(gson.toJson(message))
    }
    
    fun disconnect() {
        webSocket?.close()
        webSocket = null
    }
}
