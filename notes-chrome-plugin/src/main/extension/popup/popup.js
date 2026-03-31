/**
 * Popup script for the Notes Chrome extension (Manifest V3).
 */
document.addEventListener('DOMContentLoaded', () => {
    loadNotes();
    document.getElementById('save-btn').addEventListener('click', saveNote);
});

async function loadNotes() {
    const list    = document.getElementById('notes-list');
    const loading = document.getElementById('loading');
    try {
        const notes = await sendMessage({ action: 'getNotes' });
        loading.remove();
        if (!notes || notes.length === 0) {
            list.innerHTML = '<p>No notes yet.</p>';
            return;
        }
        notes.forEach(note => list.appendChild(createNoteElement(note)));
    } catch (e) {
        loading.textContent = 'Could not load notes. Is the server running?';
    }
}

function createNoteElement(note) {
    const el = document.createElement('div');
    el.className = 'note-item';
    el.innerHTML = `
        <span class="note-delete" data-id="${note.id}">✕</span>
        <div class="note-title">${escapeHtml(note.title)}</div>
        <div class="note-content">${escapeHtml(note.content)}</div>
    `;
    el.querySelector('.note-delete').addEventListener('click', async () => {
        await sendMessage({ action: 'deleteNote', payload: { id: note.id } });
        el.remove();
    });
    return el;
}

async function saveNote() {
    const title   = document.getElementById('note-title').value.trim();
    const content = document.getElementById('note-content').value.trim();
    if (!title) return;

    await sendMessage({ action: 'createNote', payload: { title, content } });
    document.getElementById('note-title').value   = '';
    document.getElementById('note-content').value = '';
    document.getElementById('notes-list').innerHTML = '<p id="loading">Loading notes…</p>';
    loadNotes();
}

function sendMessage(message) {
    return new Promise((resolve, reject) => {
        chrome.runtime.sendMessage(message, response => {
            if (chrome.runtime.lastError) reject(new Error(chrome.runtime.lastError.message));
            else if (response?.error)     reject(new Error(response.error));
            else                           resolve(response);
        });
    });
}

function escapeHtml(str) {
    return (str ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

