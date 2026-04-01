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
        // Escape HTML so strings like "<module>()" render literally,
        // then replace refid:123 with a clickable link to the object card.
        const safe = escapeHtml(f || '').replaceAll('\n', '<br>');
        const html = safe.replace(/refid:(\d+)/g, (_, rid) =>
            `<a href="javascript:void(0);" onclick="jumpToSlide(${rid})">${rid}</a>`
        );
        td.innerHTML = html;
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
        const valCell = createValueCell(v);
        const scope = v.scope || '';
        const id = v.id || '';

        // Name, Type
        [names, type].forEach((value) => {
            const td = document.createElement('td');
            td.textContent = value;
            tr.appendChild(td);
        });
        // Value cell with preview/full toggle when available
        tr.appendChild(valCell);
        // Scope, ID
        [scope, id].forEach((value) => {
            const td = document.createElement('td');
            td.textContent = value;
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
    return htmlize(valueDTO.kind, valueDTO.repr);
}

function htmlize(kind, text) {
    if (kind === 'primitive') return escapeHtml(text || '').replaceAll('\n', '<br>');
    const raw = text || '';
    return raw.replace(/refid:(\d+)/g, (_, rid) =>
        `<a href="javascript:void(0);" onclick="jumpToSlide(${rid})">${rid}</a>`
    ).replaceAll('\n', '<br>');
}

function createValueCell(v) {
    const td = document.createElement('td');
    const previewDiv = document.createElement('div');
    previewDiv.className = 'value-preview';
    previewDiv.innerHTML = processValue(v.value);
    td.appendChild(previewDiv);

    let fullText = v?.value?.full;
    // Optional formatting for nicer full view: lists/sets side-by-side, dicts key:value per line
    if (fullText && typeof fullText === 'string') {
        if (['list','tuple','set','dict'].includes((v.pyType || '').toLowerCase())) {
            fullText = formatFullByType(v.pyType, fullText);
        } else if (v.value.kind === 'composite') {
            fullText = formatCompositeFull(fullText);
        }
    }
    const hasExpandable = fullText && typeof fullText === 'string' && fullText.length > 0 && fullText !== v?.value?.repr;
    if (hasExpandable) {
        const fullDiv = document.createElement('div');
        fullDiv.className = 'value-full';
        fullDiv.style.display = 'none';
        fullDiv.innerHTML = htmlize(v.value.kind, fullText);
        td.appendChild(fullDiv);

        const btn = document.createElement('button');
        btn.className = 'expand-btn';
        btn.type = 'button';
        btn.textContent = 'Mehr';
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const open = fullDiv.style.display !== 'none';
            if (open) {
                fullDiv.style.display = 'none';
                previewDiv.style.display = 'block';
                btn.textContent = 'Mehr';
            } else {
                fullDiv.style.display = 'block';
                previewDiv.style.display = 'none';
                btn.textContent = 'Weniger';
            }
        });
        td.appendChild(btn);
    }
    return td;
}

// ---------- Pretty formatting for "full" ----------
function stripOuter(s, open, close) {
    s = (s || '').trim();
    if (s.startsWith(open) && s.endsWith(close)) return s.slice(1, -1);
    return s;
}

function smartSplitByComma(s) {
    const parts = [];
    let buf = '';
    let depthRound = 0, depthSquare = 0, depthCurly = 0;
    let inSingle = false, inDouble = false, escape = false;
    for (let i = 0; i < s.length; i++) {
        const ch = s[i];
        if (escape) { buf += ch; escape = false; continue; }
        if (ch === '\\') { buf += ch; escape = true; continue; }
        if (!inDouble && ch === '\'' ) { inSingle = !inSingle; buf += ch; continue; }
        if (!inSingle && ch === '"') { inDouble = !inDouble; buf += ch; continue; }
        if (!inSingle && !inDouble) {
            if (ch === '(') depthRound++;
            else if (ch === ')') depthRound = Math.max(0, depthRound-1);
            else if (ch === '[') depthSquare++;
            else if (ch === ']') depthSquare = Math.max(0, depthSquare-1);
            else if (ch === '{') depthCurly++;
            else if (ch === '}') depthCurly = Math.max(0, depthCurly-1);
            else if (ch === ',' && depthRound===0 && depthSquare===0 && depthCurly===0) {
                parts.push(buf.trim()); buf = ''; continue;
            }
        }
        buf += ch;
    }
    if (buf.trim().length) parts.push(buf.trim());
    return parts;
}

function smartSplitFirstColon(s) {
    let depthRound = 0, depthSquare = 0, depthCurly = 0;
    let inSingle = false, inDouble = false, escape = false;
    for (let i = 0; i < s.length; i++) {
        const ch = s[i];
        if (escape) { escape = false; continue; }
        if (ch === '\\') { escape = true; continue; }
        if (!inDouble && ch === '\'') { inSingle = !inSingle; continue; }
        if (!inSingle && ch === '"') { inDouble = !inDouble; continue; }
        if (!inSingle && !inDouble) {
            if (ch === '(') depthRound++;
            else if (ch === ')') depthRound = Math.max(0, depthRound-1);
            else if (ch === '[') depthSquare++;
            else if (ch === ']') depthSquare = Math.max(0, depthSquare-1);
            else if (ch === '{') depthCurly++;
            else if (ch === '}') depthCurly = Math.max(0, depthCurly-1);
            else if (ch === ':' && depthRound===0 && depthSquare===0 && depthCurly===0) {
                return i;
            }
        }
    }
    return -1;
}

function formatListLike(text, pyType) {
    let inner = (text || '').trim();
    let open = '[', close = ']';
    const t = (pyType || '').toLowerCase();
    if (t === 'tuple') { open = '('; close = ')'; }
    if (t === 'set') { open = '{'; close = '}'; }
    // Strip any existing outermost brackets to avoid duplicates
    if ((inner.startsWith('[') && inner.endsWith(']')) || (inner.startsWith('(') && inner.endsWith(')')) || (inner.startsWith('{') && inner.endsWith('}'))) {
        inner = inner.slice(1, -1);
    }
    const items = smartSplitByComma(inner);
    return `${open}${items.join(', ')}${close}`;
}

function formatDict(text) {
    let inner = stripOuter(text, '{', '}');
    const pairs = smartSplitByComma(inner);
    const lines = [];
    for (const p of pairs) {
        const idx = smartSplitFirstColon(p);
        if (idx >= 0) {
            const k = p.slice(0, idx).trim();
            const v = p.slice(idx + 1).trim();
            lines.push(`${k}: ${v}`);
        } else if (p.trim()) {
            lines.push(p.trim());
        }
    }
    return lines.join('\n');
}

function formatFullByType(pyType, text) {
    const t = (pyType || '').toLowerCase();
    if (t === 'dict') return formatDict(text);
    if (t === 'list' || t === 'tuple' || t === 'set') return formatListLike(text, pyType);
    return text;
}

function formatCompositeFull(text) {
    const lines = (text || '').split('\n');
    const out = [];
    for (const line of lines) {
        const idx = line.indexOf(':');
        if (idx <= 0) { out.push(line); continue; }
        const name = line.slice(0, idx).trim();
        let val = line.slice(idx + 1).trim();
        if (val.startsWith('{') && val.includes(':')) {
            const body = formatDict(val);
            out.push(`${name}:\n${body}`);
        } else if (val.startsWith('[') || val.startsWith('(') || (val.startsWith('{') && !val.includes(':'))) {
            // Try to infer container type brackets from current value
            let typ = 'list';
            if (val.startsWith('(')) typ = 'tuple';
            if (val.startsWith('{')) typ = 'set';
            out.push(`${name}: ${formatListLike(val, typ)}`);
        } else {
            out.push(line);
        }
    }
    return out.join('\n');
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
        // Ask server for a fresh variables snapshot (covers REPL and debug)
        setTimeout(() => sendJson('get', { resource: 'variables' }), 50);
        inputField.value = "";
    }
});

// REPL Reset button
const resetBtn = document.getElementById('repl-reset-btn');
if (resetBtn) {
    resetBtn.addEventListener('click', () => {
        sendJson('repl_reset', {});
        // Clear client-side state immediately
        try {
            document.getElementById('output-container').innerHTML = '';
            const vtbody = document.querySelector('.variables-container tbody');
            if (vtbody) vtbody.innerHTML = '';
            const ct = document.querySelector('.threads-container tbody');
            if (ct) ct.innerHTML = '';
            const slides = document.getElementById('object-slides');
            if (slides) slides.innerHTML = '';
        } catch (e) { console.error(e); }
    });
}

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
