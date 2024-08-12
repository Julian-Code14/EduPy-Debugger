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
        // Receive and proceed with the Base64 Image
        const base64Image = 'data:image/png;base64,' + event.data;
        console.log(base64Image);

        // Check if it is a correct encoded Base64 Image
        if (base64Image.startsWith('data:image/png;base64,')) {
            const base64String = base64Image.split(',')[1];
            console.log('Received Base64 string:', base64String);

            // Versuchen, das Bild anzuzeigen
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

            // Add the image to DOM to show it
            const umlOutputDiv = document.getElementById('class-diagram-container');
            umlOutputDiv.innerHTML = ''; // Empty div
            umlOutputDiv.appendChild(img);
        } else {
            console.log('Received non-image data:', base64Image);
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
