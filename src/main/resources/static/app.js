// WebSocket (JSON only)
const wsScheme = location.protocol === 'https:' ? 'wss' : 'ws';
const websocketUrl = `${wsScheme}://127.0.0.1:8025/websockets/debug`;
let socket;
const reconnectInterval = 5000;

function sendJson(type, payload = {}) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type, payload }));
    }
}

function connectWebSocket() {
    socket = new WebSocket(websocketUrl);

    socket.onopen = function () {
        console.log('WebSocket connected');
        // Initial Pulls
        sendJson('get', { resource: 'variables' });
        sendJson('get', { resource: 'object_cards' });
        sendJson('get', { resource: 'callstack' });
        sendJson('get', { resource: 'threads' });
    };

    socket.onmessage = function (event) {
        let msg;
        try {
            msg = JSON.parse(event.data);
        } catch {
            console.warn('Non-JSON ignored', event.data);
            return;
        }
        if (!msg || !msg.type) return;

        switch (msg.type) {
            case 'threads':
                renderThreads(msg.payload);
                break;
            case 'callstack':
                renderCallstack(msg.payload);
                break;
            case 'variables':
                renderVariables(msg.payload);
                break;
            case 'object_cards':
                renderObjectCards(msg.payload);
                break;
            case 'console':
                if (msg.payload && msg.payload.text) logToConsole(msg.payload.text);
                break;
            default:
                console.log('Unhandled message', msg);
        }
    };

    socket.onclose = function () {
        console.log('WebSocket disconnected');
        setTimeout(connectWebSocket, reconnectInterval);
    };

    socket.onerror = function (err) {
        console.error('WebSocket error:', err);
    };
}

/* ---------- Threads ---------- */
function renderThreads(payload) {
    const selectElement = document.getElementById('threads');
    const previousSelection = (selectElement.value || '').split(" ").at(0);
    selectElement.innerHTML = '';

    (payload?.threads || []).forEach(t => {
        const stateLabel = t.state === 'RUNNING' ? ' (läuft...)'
            : t.state === 'SUSPENDED' ? ' (angehalten)'
                : t.state === 'KILLED' ? ' (beendet)'
                    : ' (wartend...)';
        const val = t.name + stateLabel;

        const option = document.createElement('option');
        option.value = val;
        option.textContent = val;
        if (t.name === previousSelection) option.selected = true;
        selectElement.appendChild(option);
    });
}

/* ---------- Callstack ---------- */
function renderCallstack(payload) {
    const tableBody = document.querySelector('.threads-container tbody');
    tableBody.innerHTML = '';
    (payload?.frames || []).forEach(f => {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.textContent = f;
        tr.appendChild(td);
        tableBody.appendChild(tr);
    });
}

/* ---------- Variables ---------- */
let activeRow = null;

function renderVariables(payload) {
    const tableBody = document.querySelector('.variables-container tbody');
    tableBody.innerHTML = '';

    (payload?.variables || []).forEach(v => {
        const tr = document.createElement('tr');

        const names = (v.names || []).join(", ");
        const type = v.pyType || '';
        const val = processValue(v.value);
        const scope = v.scope || '';
        const id = v.id || '';

        [names, type, val, scope, id].forEach((value) => {
            const td = document.createElement('td');
            td.innerHTML = value;
            tr.appendChild(td);
        });

        tr.addEventListener('click', function (event) {
            if (event.target.tagName.toLowerCase() === 'a') return;
            if (activeRow) activeRow.classList.remove('active-row');
            tr.classList.add('active-row'); activeRow = tr;
            jumpToSlide(id);
        });

        tableBody.appendChild(tr);
    });
}

function processValue(valueDTO) {
    if (!valueDTO) return '';
    if (valueDTO.kind === 'primitive') return escapeHtml(valueDTO.repr || '');

    // 'composite': wir ersetzen "refid:123" durch klickbaren Link
    const raw = valueDTO.repr || '';
    return raw.replace(/refid:(\d+)/g, (_, rid) =>
        `<a href="javascript:void(0);" onclick="jumpToSlide(${rid})">${rid}</a>`
    ).replaceAll('\n', '<br>');
}

function escapeHtml(s) {
    const div = document.createElement('div');
    div.textContent = s; return div.innerHTML;
}

/* ---------- Object Cards (Slider) ---------- */
let currentIndex = 0;

function renderObjectCards(payload) {
    const slidesContainer = document.getElementById('object-slides');
    slidesContainer.innerHTML = '';
    (payload?.cards || []).forEach(card => {
        const slide = document.createElement('div');
        slide.classList.add('slide');
        slide.id = `slide-${card.id}`;

        const decodedSVG = atob(card.svgBase64);
        const wrapper = document.createElement('div');
        wrapper.innerHTML = decodedSVG;

        // Links in SVG „umschreiben“ -> jumpToSlide
        const svgLinks = wrapper.querySelectorAll('a');
        svgLinks.forEach(link => {
            const href = link.getAttribute('href') || '';
            const refid = href.split('/').at(1);
            link.setAttribute('href', 'javascript:void(0);');
            if (refid) link.setAttribute('onclick', `jumpToSlide(${refid})`);
        });

        slide.appendChild(wrapper);
        slidesContainer.appendChild(slide);
    });
}

function moveSlide(direction) {
    const slides = document.querySelector('#object-slides');
    const totalSlides = document.querySelectorAll('.slide').length;
    currentIndex = (currentIndex + direction + totalSlides) % totalSlides;
    slides.style.transform = `translateX(-${currentIndex * 100}%)`;
}

function jumpToSlide(refid) {
    const slides = document.querySelectorAll('.slide');
    let targetIndex = -1;
    slides.forEach((slide, index) => {
        if (slide.id === `slide-${refid}`) targetIndex = index;
    });
    if (targetIndex !== -1) {
        currentIndex = targetIndex;
        const slidesContainer = document.querySelector('#object-slides');
        slidesContainer.style.transform = `translateX(-${currentIndex * 100}%)`;
    }
}

/* ---------- Controls & UI Wiring ---------- */
connectWebSocket();

document.getElementById('resume-btn').addEventListener('click', () => sendJson('action', { command: 'resume' }));
document.getElementById('pause-btn').addEventListener('click', () => sendJson('action', { command: 'pause' }));
document.getElementById('step-over-btn').addEventListener('click', () => sendJson('action', { command: 'step-over' }));
document.getElementById('step-into-btn').addEventListener('click', () => sendJson('action', { command: 'step-into' }));
document.getElementById('step-out-btn').addEventListener('click', () => sendJson('action', { command: 'step-out' }));

// Threads Toggle
const threadsToggle = document.getElementById("threads-checkbox");
const threadsContent = document.getElementById("threads-content");
const selectElement = document.getElementById("threads");

threadsToggle.addEventListener("change", function () {
    if (threadsToggle.checked) {
        threadsContent.style.display = "block";
        const name = (selectElement.value || '').split(" ").at(0);
        sendJson('thread_selected', { name });
    } else {
        threadsContent.style.display = "none";
        sendJson('thread_selected', { name: "" });
    }
});

selectElement.addEventListener("change", function () {
    const name = (selectElement.value || '').split(" ").at(0);
    sendJson('thread_selected', { name });
});

// Object-Inspector Toggle
const oiToggleBtn = document.getElementById('oi-toggle');
const oiContainer = document.getElementById('object-inspector-container');
oiToggleBtn.addEventListener('click', () => {
    oiContainer.classList.toggle('collapsed');
    oiToggleBtn.classList.toggle('rotated');
});

// Console
document.getElementById("console-input").addEventListener("keydown", function (event) {
    const inputField = document.getElementById("console-input");
    if (event.key === "Enter") {
        const inputValue = inputField.value;
        logToConsole(inputValue, true);
        sendJson('console_input', { text: inputValue });
        inputField.value = "";
    }
});

function logToConsole(message, isPrompt = false) {
    const outputContainer = document.getElementById("output-container");
    const newEntry = document.createElement('div');
    newEntry.classList.add('log-entry');
    newEntry.textContent = (isPrompt ? "> " : "") + message;
    outputContainer.appendChild(newEntry);
    outputContainer.scrollTop = outputContainer.scrollHeight;
}

/* Sticky controls */
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

/* Navigation buttons (unchanged) */
function goToClassDiagram() {
    window.location.href = 'pages/class-diagram.html';
    try { socket.close(); } catch (e) { console.error(e); }
}
function goToObjectDiagram() {
    window.location.href = 'pages/object-diagram.html';
    try { socket.close(); } catch (e) { console.error(e); }
}
function goToPythonTutor() {
    window.open('https://pythontutor.com/python-compiler.html#mode=edit', '_blank');
}