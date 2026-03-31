" =============================================================================
" notes.vim - Vim plugin for the Notes application
" Author: Notes Project
" License: MIT
" =============================================================================

if exists('g:loaded_notes')
    finish
endif
let g:loaded_notes = 1

" Default configuration
let g:notes_api_url = get(g:, 'notes_api_url', 'http://localhost:8080/api/notes')
let g:notes_split    = get(g:, 'notes_split', 'vertical')

" --------------------------------------------------------------------------
" Commands
" --------------------------------------------------------------------------
command! NotesNew     call notes#new()
command! NotesList    call notes#list()
command! NotesDelete  call notes#delete()
command! NotesSearch  call notes#search()

" --------------------------------------------------------------------------
" Key mappings (all prefixed with <Leader>n)
" --------------------------------------------------------------------------
nnoremap <silent> <Leader>nn :NotesNew<CR>
nnoremap <silent> <Leader>nl :NotesList<CR>
nnoremap <silent> <Leader>nd :NotesDelete<CR>
nnoremap <silent> <Leader>ns :NotesSearch<CR>

