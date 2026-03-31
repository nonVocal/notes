/**
 * Service Worker for the Notes Chrome extension (Manifest V3).
 * Handles communication with the Notes REST API.
 */

const API_BASE = 'http://localhost:8080/api/notes';

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    handleMessage(message)
        .then(sendResponse)
        .catch(err => sendResponse({ error: err.message }));
    return true; // keep message channel open for async response
});

async function handleMessage(message) {
    switch (message.action) {
        case 'getNotes':
            return fetchNotes();
        case 'createNote':
            return createNote(message.payload);
        case 'deleteNote':
            return deleteNote(message.payload.id);
        default:
            throw new Error('Unknown action: ' + message.action);
    }
}

async function fetchNotes() {
    const response = await fetch(API_BASE);
    if (!response.ok) throw new Error('Failed to fetch notes');
    return response.json();
}

async function createNote(note) {
    const response = await fetch(API_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(note)
    });
    if (!response.ok) throw new Error('Failed to create note');
    return response.json();
}

async function deleteNote(id) {
    const response = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
    if (!response.ok) throw new Error('Failed to delete note');
    return { success: true };
}

