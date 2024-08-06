// Websocket
const websocketUrl = 'ws://localhost:8025/websockets/debug'
let socket;
const reconnectInterval = 5000;

function connectWebSocket() {
    socket = new WebSocket(websocketUrl);

    socket.onmessage = function(event) {
        //debugInfoDiv.textContent = event.data;

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
                console.log('Image loaded successfully.');
            };
            img.onerror = function(error) {
                console.error('Failed to load image:', error);
            };

            // Add the image to DOM to show it
            const umlOutputDiv = document.getElementById('uml-output');
            umlOutputDiv.innerHTML = ''; // Empty div
            umlOutputDiv.appendChild(img);
        } else {
            console.log('Received non-image data:', base64Image);
        }
    };

    socket.onopen = function() {
        console.log('WebSocket connection established');
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


// Plant UML
/*document.getElementById('render-button').addEventListener('click', function() {
    const umlInput = document.getElementById('uml-input').value;
    if (socket.readyState === WebSocket.OPEN) {
        socket.send(umlInput);
        console.log('UML data sent to server via websocket');
    } else {
        console.error('Websocket is not open');
    }
});*/

// Controls
document.getElementById('resume-btn').addEventListener('click', function() {
    if (socket.readyState === WebSocket.OPEN) {
        socket.send("action:resume")
        console.log('Sent resume action');
    } else {
        console.error('WebSocket connection closed');
    }
});
document.getElementById('pause-btn').addEventListener('click', function() {
    if (socket.readyState === WebSocket.OPEN) {
        socket.send("action:pause")
        console.log('Sent pause action');
    } else {
        console.error('WebSocket connection closed');
    }
});
document.getElementById('step-over-btn').addEventListener('click', function() {
    if (socket.readyState === WebSocket.OPEN) {
        socket.send("action:step-over")
        console.log('Sent step over action');
    } else {
        console.error('WebSocket connection closed');
    }
});
document.getElementById('step-into-btn').addEventListener('click', function() {
    if (socket.readyState === WebSocket.OPEN) {
        socket.send("action:step-into")
        console.log('Sent step into action');
    } else {
        console.error('WebSocket connection closed');
    }
});
document.getElementById('step-out-btn').addEventListener('click', function() {
    if (socket.readyState === WebSocket.OPEN) {
        socket.send("action:step-out")
        console.log('Sent step-out action');
    } else {
        console.error('WebSocket connection closed');
    }
});
