const websocketUrl = 'ws://localhost:8025/websockets/debug';
let socket;
const reconnectInterval = 5000;

function goToIndex() {
    window.location.href = '../index.html';
    try { socket.close(); } catch (e) { console.error(e); }
}

function sendJson(type, payload = {}) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type, payload }));
    }
}

function connectWebSocket() {
    socket = new WebSocket(websocketUrl);

    socket.onopen = function () {
        console.log('WebSocket connected');
        sendJson('get', { resource: 'class_diagram' });
    };

    socket.onmessage = function (event) {
        let msg;
        try { msg = JSON.parse(event.data); } catch { return; }
        if (!msg || msg.type !== 'class_diagram') return;

        const base64Data = msg.payload?.svgBase64 || '';
        const container = document.getElementById('class-diagram-container');
        container.innerHTML = '';

        const decodedSVG = atob(base64Data);
        const svgElement = document.createElement('div');
        svgElement.innerHTML = decodedSVG;
        container.appendChild(svgElement);
    };

    socket.onclose = function () {
        console.log('WebSocket closed');
        setTimeout(connectWebSocket, reconnectInterval);
    };

    socket.onerror = function (error) {
        console.error('WebSocket error:', error);
    };
}

connectWebSocket();