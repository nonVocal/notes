/**
 * Notes extension UI – view logic shared by popup (dialog) and sidebar.
 * Uses chrome.runtime.sendMessage (Chrome MV3 service worker).
 */
document.addEventListener('DOMContentLoaded', () => {
    let allNotes = [];

    // ── View routing ───────────────────────────────────────────────────────
    function showView(name) {
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        document.querySelectorAll('#sidebar-nav button').forEach(b => b.classList.remove('active'));
        document.getElementById('view-' + name)?.classList.add('active');
        document.querySelector(`#sidebar-nav [data-view="${name}"]`)?.classList.add('active');
        if (name === 'all-notes') loadNotes();
        if (name === 'search')    setTimeout(() => document.getElementById('search-input')?.focus(), 30);
        if (name === 'new-note')  setTimeout(() => document.getElementById('title')?.focus(), 30);
    }

    document.querySelectorAll('#sidebar-nav button').forEach(btn =>
        btn.addEventListener('click', () => showView(btn.dataset.view)));

    // ── Ctrl+F → Search (works inside popup/sidebar focus context) ─────────
    document.addEventListener('keydown', e => {
        if (e.ctrlKey && e.key === 'f') { e.preventDefault(); showView('search'); }
    });

    // ── "Open as Sidebar" button (popup only – absent in sidebar.html) ──────
    document.getElementById('open-sidebar-btn')?.addEventListener('click', () => {
        chrome.tabs.query({ active: true, currentWindow: true }, tabs => {
            if (tabs[0] && chrome.sidePanel) {
                chrome.sidePanel.open({ tabId: tabs[0].id });
            }
        });
        window.close();
    });

    // ── Background messaging ───────────────────────────────────────────────
    function sendMsg(action, payload) {
        return new Promise((resolve, reject) => {
            chrome.runtime.sendMessage({ action, payload }, response => {
                if (chrome.runtime.lastError) reject(new Error(chrome.runtime.lastError.message));
                else if (response?.error)     reject(new Error(response.error));
                else                           resolve(response);
            });
        });
    }

    // ── Load & render notes ────────────────────────────────────────────────
    async function loadNotes() {
        const list = document.getElementById('notes-list');
        if (!list) return;
        list.innerHTML = '<p class="empty-hint">Loading…</p>';
        try {
            allNotes = await sendMsg('getNotes');
            renderNotes(allNotes, list);
        } catch {
            list.innerHTML = '<p class="err">Could not load notes.</p>';
        }
    }

    function renderNotes(notes, container) {
        if (!notes?.length) { container.innerHTML = '<p class="empty-hint">No notes yet.</p>'; return; }
        container.innerHTML = '';
        notes.forEach(n => container.appendChild(buildCard(n)));
    }

    function buildCard(note) {
        const div = document.createElement('div');
        div.className = 'note-card';
        div.innerHTML = `
            <div class="note-card-title">${esc(note.title)}</div>
            <div class="note-card-content">${esc(note.content ?? '')}</div>
            <div class="note-card-footer"><button class="btn btn-danger">Delete</button></div>`;
        div.querySelector('button').onclick = async () => {
            await sendMsg('deleteNote', { id: note.id });
            div.remove();
            allNotes = allNotes.filter(n => n.id !== note.id);
            runSearch();
        };
        return div;
    }

    // ── Save note ──────────────────────────────────────────────────────────
    document.getElementById('save-btn')?.addEventListener('click', async () => {
        const titleEl = document.getElementById('title');
        const contEl  = document.getElementById('note-content');
        const title   = titleEl?.value.trim() ?? '';
        const content = contEl?.value.trim() ?? '';
        if (!title) { setStatus('Title is required.', 'error'); return; }
        try {
            await sendMsg('createNote', { title, content });
            titleEl.value = ''; contEl.value = '';
            setStatus('Saved!', 'success');
            setTimeout(() => showView('all-notes'), 700);
        } catch { setStatus('Save failed.', 'error'); }
    });

    function setStatus(msg, type) {
        const el = document.getElementById('status');
        if (el) { el.textContent = msg; el.className = type; }
    }

    document.getElementById('title')?.addEventListener('keydown', e => {
        if (e.key === 'Enter') { e.preventDefault(); document.getElementById('note-content')?.focus(); }
    });

    // ── Search ─────────────────────────────────────────────────────────────
    function runSearch() {
        const q       = document.getElementById('search-input')?.value.trim().toLowerCase() ?? '';
        const results = document.getElementById('search-results');
        if (!results) return;
        if (!q) { results.innerHTML = ''; return; }
        const hits = allNotes.filter(n =>
            n.title.toLowerCase().includes(q) || (n.content ?? '').toLowerCase().includes(q));
        renderNotes(hits, results);
    }

    document.getElementById('search-input')?.addEventListener('input',   runSearch);
    document.getElementById('search-input')?.addEventListener('keydown', e => { if (e.key === 'Enter') runSearch(); });

    // ── Helpers ────────────────────────────────────────────────────────────
    function esc(s) { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

    loadNotes();
});


