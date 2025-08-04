// Websocket
const websocketUrl = 'ws://localhost:8025/websockets/debug'
let socket;
const reconnectInterval = 5000;

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

function updateThreadOptions(dataString) {
    // Referenz auf das <select>-Element
    const selectElement = document.getElementById('threads');

    // Bisher ausgewählten Wert merken
    const previousSelection = selectElement.value.split(" ").at(0);

    // In Array umwandeln und leere Einträge rausfiltern
    const threads = dataString.split(';').filter(entry => entry !== '');

    // Vorherige Optionen entfernen
    selectElement.innerHTML = '';

    // Neue Optionen anhand der Threads hinzufügen
    threads.forEach(thread => {
        const option = document.createElement('option');
        option.value = thread;
        option.textContent = thread;

        // Wenn der Thread dem alten Wert entspricht, Option ausgewählt markieren
        if (thread === previousSelection) {
            option.selected = true;
        }

        selectElement.appendChild(option);
    });
}

function updateCallStackTable(dataString) {
    const tableBody = document.querySelector('.threads-container tbody');
    tableBody.innerHTML = ''; // Clear existing rows

    const rows = dataString.split(';');
    rows.forEach(row => {
        if (row.trim() === '') return;
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.innerHTML = row;
        tr.appendChild(td);

        tableBody.appendChild(tr);
    });
}

let activeRow = null; // Variable to keep track of the currently active row

function updateVariablesTable(dataString) {
    const tableBody = document.querySelector('.variables-container tbody');
    tableBody.innerHTML = ''; // Clear existing rows

    const rows = dataString.split(';');
    rows.forEach(row => {
        if (row.trim() === '') return;

        const [id, rest] = row.split('=');
        const [name, type, currentValue, scope] = rest.split(',');

        const tr = document.createElement('tr');
        [extractNameList(name), type, processCellContent(extractComplexValue(currentValue)), scope, id].forEach((value, index) => {
            const td = document.createElement('td');
            td.innerHTML = value;
            tr.appendChild(td);
        });

        // Add click event to the row to use the ID from the last cell to jump to the corresponding slide
        tr.addEventListener('click', function(event) {
            // Check if the target is a link (<a>), if so, don't trigger row click
            if (event.target.tagName.toLowerCase() === 'a') {
                return; // If the click is on a link, do nothing (let the link work)
            }

            // Remove active class from the previously active row
            if (activeRow) {
                activeRow.classList.remove('active-row');
            }

            // Mark the clicked row as active
            tr.classList.add('active-row');
            activeRow = tr; // Store the active row

            jumpToSlide(id);  // Jump to the slide with this ID
        });

        tableBody.appendChild(tr); // Append the row to the table body
    });
}

function processCellContent(content) {
    // This function looks for the pattern refid:1234 and replaces it with a link to the Object Card
    const refidPattern = /refid:(\d+)/g;
    return content.replace(refidPattern, function(match, refid) {
        // Replace with an HTML anchor link that calls jumpToSlide with the refid
        return `<a href="javascript:void(0);" onclick="jumpToSlide(${refid})">${refid}</a>`;
    });
}


function extractNameList(dataString) {
    const names = dataString.split('###');
    return names.join(", ");
}

function extractComplexValue(dataString) {
    return dataString.replaceAll("|", ": ").replaceAll("###", "<br>").replaceAll("~", ",<br>");
}

let currentIndex = 0;

function moveSlide(direction) {
    const slides = document.querySelector('#object-slides');
    const totalSlides = document.querySelectorAll('.slide').length;

    currentIndex = (currentIndex + direction + totalSlides) % totalSlides;
    slides.style.transform = `translateX(-${currentIndex * 100}%)`;
}

function jumpToSlide(refid) {
    const slides = document.querySelectorAll('.slide');
    let targetIndex = -1;

    // Find the index of the slide with the corresponding refid
    slides.forEach((slide, index) => {
        if (slide.id === `slide-${refid}`) {
            targetIndex = index;
        }
    });

    if (targetIndex !== -1) {
        // Update the currentIndex to the targetIndex and move the slider
        currentIndex = targetIndex;
        const slidesContainer = document.querySelector('#object-slides');
        slidesContainer.style.transform = `translateX(-${currentIndex * 100}%)`;
    }
}


function updateObjectCardImages(dataString) {
    const slidesContainer = document.getElementById('object-slides');
    slidesContainer.innerHTML = ''; // Empty div

    // Split the data string by '###' to get each image block
    const imageBlocks = dataString.split('###').filter(block => block.trim() !== '');

    imageBlocks.forEach(block => {
        const [id, base64Data] = block.split('|');

        // Check if it's a PNG or an SVG
        if (base64Data.startsWith('iVBORw0KGgo')) {
            // PNG handling
            const base64Image = 'data:image/png;base64,' + base64Data;
            const img = new Image();
            img.src = base64Image;
            img.onload = function () {
                socket.send("Success: Image loaded successfully with ID " + id);
                console.log('Image loaded successfully with ID:', id);
            };
            img.onerror = function (error) {
                socket.send("Client: Failed to load image with ID " + id + ": " + error);
                console.error('Failed to load image with ID:', id, error);
            };
            const slide = document.createElement('div');
            slide.classList.add('slide');
            slide.id = `slide-${id}`;
            slide.appendChild(img);
            slidesContainer.appendChild(slide);
        } else {
            // SVG handling
            const decodedSVG = atob(base64Data);  // Decode the base64-encoded SVG
            const svgElement = document.createElement('div');
            svgElement.classList.add('slide');
            svgElement.id = `slide-${id}`;

            // Insert the SVG into the DOM
            svgElement.innerHTML = decodedSVG;

            // Add onclick to links in SVG dynamically
            const svgLinks = svgElement.querySelectorAll('a');
            svgLinks.forEach(link => {
                const refid = link.getAttribute('href').split("/").at(1);
                link.setAttribute('href', 'javascript:void(0);'); // Avoid default behavior
                link.setAttribute('onclick', `jumpToSlide(${refid})`); // Attach the JS function
            });

            slidesContainer.appendChild(svgElement); // Add the slide to the slider
        }
    });
}


function connectWebSocket() {
    socket = new WebSocket(websocketUrl);

    socket.onmessage = function(event) {
        const eventData = splitStringAtFirstColon(event.data);

        switch (eventData.at(0)) {
            case "threads:":
                updateThreadOptions(eventData.at(1));
                break;
            case "callstack:":
                updateCallStackTable(eventData.at(1));
                break;
            case "variables:":
                updateVariablesTable(eventData.at(1));
                break;
            case "oc:":
                updateObjectCardImages(eventData.at(1));
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
document.addEventListener('DOMContentLoaded', function () {
    const controls = document.querySelector('.controls');
    const initialOffsetTop = controls.offsetTop;

    window.addEventListener('scroll', function () {
        if (window.scrollY > initialOffsetTop) {
            controls.classList.add('sticky-controls');
        } else {
            controls.classList.remove('sticky-controls');
        }
    });
});

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

// Threads Toggle
const threadsToggle = document.getElementById("threads-checkbox");

// Füge den "change"-EventListener hinzu:
threadsToggle.addEventListener("change", function() {
    if (threadsToggle.checked) {
        threadsActivated();
    } else {
        threadsDeactivated();
    }
});

const threadsContent = document.getElementById("threads-content");

function threadsActivated() {
    threadsContent.style.display = "block";
    socket.send("action:thread-selected:" + selectElement.value.split(" ").at(0));
}

function threadsDeactivated() {
    threadsContent.style.display = "none";
    socket.send("action:thread-selected:");
}

const selectElement = document.getElementById("threads");

selectElement.addEventListener("change", function() {
    // Ausgewählten Wert ermitteln
    const selectedValue = selectElement.value.split(" ").at(0);

    // Value direkt an den Server schicken
    if (socket.readyState === WebSocket.OPEN) {
        socket.send("action:thread-selected:" + selectedValue);
        console.log('Thread-Request sent for: ' + selectedValue);
    }
});

// Object-Inspector Toggle
const oiToggleBtn   = document.getElementById('oi-toggle');
const oiContainer   = document.getElementById('object-inspector-container');

oiToggleBtn.addEventListener('click', () => {
    oiContainer.classList.toggle('collapsed');   // Höhe umschalten
    oiToggleBtn.classList.toggle('rotated');     // Pfeil drehen
});

// Console
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

function logToConsole(message, isPrompt = false) {
    const outputContainer = document.getElementById("output-container");
    const newEntry = document.createElement('div');
    newEntry.classList.add('log-entry'); // Für Animation
    newEntry.textContent = (isPrompt ? "> " : "") + message;
    outputContainer.appendChild(newEntry); // Neue Ausgabe hinzufügen
    outputContainer.scrollTop = outputContainer.scrollHeight;  // Auto-Scroll nach unten
}

// Bottom Buttons
function goToClassDiagram() {
    window.location.href = 'pages/class-diagram.html';
    try {
        socket.close();
    } catch (e) {
        console.error(e);
    }
}

function goToObjectDiagram() {
    window.location.href = 'pages/object-diagram.html';
    try {
        socket.close();
    } catch (e) {
        console.error(e);
    }
}

function goToPythonTutor() {
    window.open('https://pythontutor.com/python-compiler.html#mode=edit', '_blank');
}
