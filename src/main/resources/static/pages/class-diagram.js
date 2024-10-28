/*
* @author julian
* @version 0.2.0
* @since 0.1.0
* */

// Websocket
const websocketUrl = 'ws://localhost:8025/websockets/debug'
let socket;
const reconnectInterval = 5000;

function goToIndex() {
    window.location.href = '../index.html';
    try {
        socket.close();
    } catch (e) {
        console.error(e);
    }
}

function connectWebSocket() {
    socket = new WebSocket(websocketUrl);

    socket.onmessage = function(event) {
        const base64Data = event.data;

        const container = document.getElementById('class-diagram-container');
        container.innerHTML = ''; // Container leeren

        // Prüfe, ob die empfangenen Daten SVG oder PNG sind
        if (base64Data.startsWith('iVBORw0KGgo')) {
            // PNG-Daten werden verarbeitet
            const base64Image = 'data:image/png;base64,' + base64Data;
            const img = new Image();
            img.src = base64Image;
            img.onload = function() {
                socket.send("Success: Image loaded successfully.");
                console.log('Image loaded successfully.');
            };
            img.onerror = function(error) {
                socket.send("Client: Failed to load image: " + error);
                console.error('Failed to load image:', error);
            };
            container.appendChild(img); // Bild hinzufügen
        } else {
            // SVG-Daten werden verarbeitet
            const decodedSVG = atob(base64Data); // Base64-Dekodierung des SVG
            const svgElement = document.createElement('div');
            svgElement.innerHTML = decodedSVG;

            container.appendChild(svgElement); // SVG zur Anzeige hinzufügen
        }
    };

    socket.onopen = function() {
        console.log('WebSocket connection established');
        if (socket.readyState === WebSocket.OPEN) {
            socket.send("get:cd");
        }
    };

    socket.onclose = function() {
        console.log('WebSocket connection closed');
        // Try to reconnect to the websocket server
        setTimeout(connectWebSocket, reconnectInterval);
    };

    socket.onerror = function(error) {
        console.error('WebSocket error:', error);
    };
}


// Initial Websocket Connection
connectWebSocket();
