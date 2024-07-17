// Websocket
//const debugInfoDiv = document.getElementById('debug-info');
const socket = new WebSocket('ws://localhost:8025/websockets/debug');

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
};

socket.onerror = function(error) {
    console.error('WebSocket error:', error);
};


// Plant UML
document.getElementById('render-button').addEventListener('click', function() {
    const umlInput = document.getElementById('uml-input').value;
    if (socket.readyState === WebSocket.OPEN) {
        socket.send(umlInput);
        console.log('UML data sent to server via websocket');
    } else {
        console.error('Websocket is not open');
    }
});
