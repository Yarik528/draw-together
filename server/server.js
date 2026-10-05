const express = require('express');
const http = require('http');
const WebSocket = require('ws');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const rooms = new Map();

app.get('/', (req, res) => {
  res.send('🎨 Draw Together server is running!');
});

wss.on('connection', (ws) => {
    ws.room = null;
    
    ws.on('message', (data) => {
        try {
            const message = JSON.parse(data);
            
            if (message.type === 'join') {
                const roomCode = message.roomCode;
                
                if (!rooms.has(roomCode)) {
                    rooms.set(roomCode, {
                        clients: new Set(),
                        strokes: new Map()
                    });
                }
                
                const room = rooms.get(roomCode);
                room.clients.add(ws);
                ws.room = roomCode;
                
                const allStrokes = Array.from(room.strokes.values());
                ws.send(JSON.stringify({
                    type: 'history',
                    strokes: allStrokes
                }));
                
                console.log(`User joined room ${roomCode}. Total: ${room.clients.size}`);
            }
            
            if (message.type === 'stroke') {
                const room = rooms.get(ws.room);
                if (room) {
                    room.strokes.set(message.stroke.id, message.stroke);
                    broadcast(ws.room, message, ws);
                }
            }
            
            if (message.type === 'clear') {
                const room = rooms.get(ws.room);
                if (room) {
                    room.strokes.clear();
                    broadcast(ws.room, message, ws);
                }
            }
            
        } catch (e) {
            console.error('Error:', e);
        }
    });
    
    ws.on('close', () => {
        if (ws.room) {
            const room = rooms.get(ws.room);
            if (room) {
                room.clients.delete(ws);
                if (room.clients.size === 0) {
                    rooms.delete(ws.room);
                    console.log(`Room ${ws.room} deleted`);
                }
            }
        }
    });
});

function broadcast(roomCode, message, excludeWs = null) {
    const room = rooms.get(roomCode);
    if (!room) return;
    
    const data = JSON.stringify(message);
    room.clients.forEach((client) => {
        if (client !== excludeWs && client.readyState === WebSocket.OPEN) {
            client.send(data);
        }
    });
}

const PORT = process.env.PORT || 10000;
server.listen(PORT, () => {
    console.log(`🎨 Server running on port ${PORT}`);
});
