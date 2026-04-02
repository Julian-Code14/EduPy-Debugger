/**
 * EduPy Debugger Frontend Script
 *
 * Responsibilities
 * - Maintain a WebSocket to the IDE backend and route incoming typed payloads.
 * - Render sections: threads, callstack, variables, and object inspector (cards/diagram).
 * - Provide compact previews in the variables table and on‑demand expansion with readable formatting.
 * - Wire basic debugger controls (resume/pause/step) and a lightweight REPL console.
 *
 * Conventions
 * - All outbound messages use { type, payload } JSON; inbound messages follow the same schema.
 * - For variables, ValueDTO.repr is a preview; ValueDTO.full (when present) contains the full string.
 */
// WebSocket (JSON only)
const wsScheme = location.protocol === 'https:' ? 'wss' : 'ws';
const websocketUrl = `${wsScheme}://127.0.0.1:8025/websockets/debug`;
let socket;
const reconnectInterval = 5000;

/**
 * Sends a typed JSON message to the backend if the socket is open.
 * @param {string} type Message type (e.g., 'get', 'action').
 * @param {object} payload JSON payload (optional).
 */
function sendJson(type, payload = {}) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type, payload }));
    }
}

/**
 * Establishes the WebSocket connection and dispatches inbound messages
 * to their respective renderers.
 */
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
/**
 * Renders the threads dropdown and the callstack table header.
 * @param {{threads: Array<{name:string,state:string}>}} payload
 */
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
/**
 * Renders the callstack table.
 * @param {{frames: string[]}} payload
 */
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

/**
 * Renders the variables table (name, type, value preview/full, scope, id).
 * @param {{variables: Array}} payload
 */
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

/**
 * Converts a ValueDTO into HTML string for preview rendering.
 * Replaces refid:### with clickable links to object cards.
 * @param {{kind:string, repr:string}} valueDTO
 * @returns {string}
 */
function processValue(valueDTO) {
    if (!valueDTO) return '';
    return htmlize(valueDTO.kind, valueDTO.repr);
}

/**
 * Escapes/expands a text representation to HTML.
 * - For primitives: escape and preserve newlines.
 * - For composites: also convert refid:### to object-card links.
 * @param {'primitive'|'composite'} kind
 * @param {string} text
 * @returns {string}
 */
function htmlize(kind, text) {
    if (kind === 'primitive') return escapeHtml(text || '').replaceAll('\n', '<br>');
    const raw = text || '';
    return raw.replace(/refid:(\d+)/g, (_, rid) =>
        `<a href="javascript:void(0);" onclick="jumpToSlide(${rid})">${rid}</a>`
    ).replaceAll('\n', '<br>');
}

/**
 * Builds the value cell with preview and optional full view + toggle button.
 * @param {{pyType:string, value:{kind:string, repr:string, full?:string}}} v
 * @returns {HTMLTableCellElement}
 */
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
/** Removes a matching outer pair of brackets if present. */
function stripOuter(s, open, close) {
    s = (s || '').trim();
    if (s.startsWith(open) && s.endsWith(close)) return s.slice(1, -1);
    return s;
}

/**
 * Splits a string on commas at top level, ignoring nested brackets and quotes.
 * Useful for pretty-printing list/tuple/set/dict representations.
 */
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

/** Finds the first top-level colon position in a string, or -1. */
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

/** Formats list/tuple/set as one line, keeping appropriate brackets. */
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

/** Formats a dict as {\nkey: value\n...\n}. */
function formatDict(text) {
    let inner = stripOuter(text, '{', '}');
    if (!inner.trim()) return '{}';
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
    return '{\n' + lines.join('\n') + '\n}';
}

/** Dispatches pretty formatting by Python type name. */
function formatFullByType(pyType, text) {
    const t = (pyType || '').toLowerCase();
    if (t === 'dict') return formatDict(text);
    if (t === 'list' || t === 'tuple' || t === 'set') return formatListLike(text, pyType);
    return text;
}

/** Applies pretty formatting per attribute line for composite previews. */
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
            out.push(`${name}: ${body}`);
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

    // Ensure Object‑Inspector is visible and scroll into view
    try {
        const oiContainer = document.getElementById('object-inspector-container');
        const oiToggleBtn = document.getElementById('oi-toggle');
        if (oiContainer) {
            if (oiContainer.classList.contains('collapsed')) {
                oiContainer.classList.remove('collapsed');
                if (oiToggleBtn) oiToggleBtn.classList.remove('rotated');
            }
            const y = oiContainer.getBoundingClientRect().top + window.pageYOffset - 20; // small offset for sticky controls
            window.scrollTo({ top: y, behavior: 'smooth' });
        }
    } catch (e) { console.error(e); }
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
    if (!controls) return;
    const initialOffsetTop = controls.offsetTop;
    const toggleFloating = () => {
        const y = window.scrollY || window.pageYOffset;
        if (y > initialOffsetTop + 4) controls.classList.add('controls-floating');
        else controls.classList.remove('controls-floating');
    };
    window.addEventListener('scroll', toggleFloating, { passive: true });
    toggleFloating();
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
