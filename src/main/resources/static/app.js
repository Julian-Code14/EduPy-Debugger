// Websocket
const websocketUrl = 'ws://localhost:8025/websockets/debug'
let socket;
const reconnectInterval = 5000;

function logToConsole(message, isPrompt = false) {
    const outputContainer = document.getElementById("output-container");
    const newEntry = document.createElement('div');
    newEntry.classList.add('log-entry'); // Für Animation
    newEntry.textContent = (isPrompt ? "> " : "") + message;
    outputContainer.appendChild(newEntry); // Neue Ausgabe hinzufügen
    outputContainer.scrollTop = outputContainer.scrollHeight;  // Auto-Scroll nach unten
}

function goToClassDiagram() {
    window.location.href = 'pages/class-diagram.html';
    try {
        socket.close();
    } catch (e) {
        console.error(e);
    }
}

function goToPythonTutor() {
    socket.send("navigate:")
    try {
        socket.close();
    } catch (e) {
        console.error(e);
    }
}

function splitStringAtFirstColon(input) {
    // Finden des Index des ersten Doppelpunkts
    const index = input.indexOf(':');

    // Wenn kein Doppelpunkt gefunden wird, kann der String nicht geteilt werden
    if (index === -1) {
        return [input, ''];
    }

    // Der Doppelpunkt gehört zur ersten Hälfte
    const firstPart = input.slice(0, index + 1); // inkl. Doppelpunkt
    const secondPart = input.slice(index + 1);   // nach dem Doppelpunkt

    return [firstPart, secondPart];
}

function updateVariablesTable(dataString) {
    // Get the table body element
    const tableBody = document.querySelector('.variables-container tbody');

    // Clear the existing rows
    tableBody.innerHTML = '';

    // Split the data string by ';' to get each row
    const rows = dataString.split(';');

    // Iterate over each row and create table rows
    rows.forEach(row => {
        if (row.trim() === '') return; // Skip empty rows

        // Split the row into columns by '=' and ','
        const [id, rest] = row.split('=');
        const [name, type, currentValue, scope] = rest.split(',');

        // Create a new row element
        const tr = document.createElement('tr');

        // Create and append cell elements for each value
        [id, name, type, currentValue, scope].forEach(value => {
            const td = document.createElement('td');
            td.textContent = value;
            tr.appendChild(td);
        });

        // Append the row to the table body
        tableBody.appendChild(tr);
    });
}

function updateObjectCardsImage(dataString) {
    // Receive and proceed with the Base64 Image
    const base64Image = 'data:image/png;base64,' + dataString;
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
        const umlOutputDiv = document.getElementById('object-inspector-container');
        umlOutputDiv.innerHTML = ''; // Empty div
        umlOutputDiv.appendChild(img);
    } else {
        socket.send("Client: Received non-image data");
        console.log('Received non-image data:', base64Image);
    }
}


function connectWebSocket() {
    socket = new WebSocket(websocketUrl);

    socket.onmessage = function(event) {
        const eventData = splitStringAtFirstColon(event.data);

        switch (eventData.at(0)) {
            case "variables:":
                updateVariablesTable(eventData.at(1));
                break;
            case "oc:":
                updateObjectCardsImage(eventData.at(1));
                break;
            case "od:":
                // TODO: Bring back object diagrams at another level
                break;
            case "console:":
                logToConsole(eventData.at(1));
                break;
            default:
                console.error(eventData);
        }

    };

    socket.onopen = function() {
        console.log('WebSocket connection established');
        if (socket.readyState === WebSocket.OPEN) {
            socket.send("get:variables")
            socket.send("get:oc");
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

// TODO: get:od und get:oc wurden früher durch Drücken des Switches angefordert - wird das noch benötigt?

document.getElementById("console-input").addEventListener("keydown", function(event) {
    const inputField = document.getElementById("console-input");

    // Wenn Enter gedrückt wird
    if (event.key === "Enter") {
        const inputValue = inputField.value;
        logToConsole(inputValue); // Eingabe anzeigen
        if (socket.readyState === WebSocket.OPEN) {
            socket.send("action:console-input:" + inputValue)
            console.log('Sent console action');
        } else {
            console.error('WebSocket connection closed');
        }
        inputField.value = "";  // Eingabefeld leeren
    }
});
