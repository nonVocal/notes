/**
 * Background script for the Notes Firefox extension.
 * Handles communication with the Notes REST API.
 */

const API_BASE = 'http://localhost:8080/api/notes';

// Listen for messages from popup or content scripts
browser.runtime.onMessage.addListener((message, sender) => {
    switch (message.action) {
        case 'getNotes':
            return fetchNotes();
        case 'createNote':
            return createNote(message.payload);
        case 'deleteNote':
            return deleteNote(message.payload.id);
        default:
            console.warn('Unknown action:', message.action);
    }
});

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
}

