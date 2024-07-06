// Websocket
//const debugInfoDiv = document.getElementById('debug-info');
const socket = new WebSocket('ws://localhost:8025/websockets/debug');

socket.onmessage = function(event) {
    //debugInfoDiv.textContent = event.data;

    const base64Image = event.data;
    console.log(base64Image);
    const imageUrl = `data:image/png;base64,${base64Image}`;
    console.log(imageUrl);
    document.getElementById('uml-output').innerHTML = `<img src="${imageUrl}" alt="PlantUML Diagram">`;
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
