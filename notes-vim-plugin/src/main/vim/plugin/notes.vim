" =============================================================================
" notes.vim - Vim plugin for the Notes application
" Author: Notes Project
" License: MIT
" =============================================================================

if exists('g:loaded_notes')
    finish
endif
let g:loaded_notes = 1

" --------------------------------------------------------------------------
" Configuration  (override any of these in your vimrc before the plugin loads)
" --------------------------------------------------------------------------
let g:notes_api_url       = get(g:, 'notes_api_url',       'http://localhost:8080/api/notes')
let g:notes_split         = get(g:, 'notes_split',         'vertical')
let g:notes_sidebar_width = get(g:, 'notes_sidebar_width', 26)
" Set to 0 to prevent the plugin from touching 'mouse'
let g:notes_mouse         = get(g:, 'notes_mouse',         1)

if g:notes_mouse
    set mouse=a
endif

" --------------------------------------------------------------------------
" Commands
" --------------------------------------------------------------------------
command! NotesOpen    call notes#open_sidebar()
command! NotesClose   call notes#close_sidebar()
command! NotesToggle  call notes#toggle_sidebar()
command! NotesNew     call notes#cmd_new()
command! NotesList    call notes#cmd_list()
command! NotesDelete  call notes#cmd_delete()
command! NotesSearch  call notes#cmd_search()

" --------------------------------------------------------------------------
" Key mappings  (<Leader>n prefix)
" --------------------------------------------------------------------------
nnoremap <silent> <Leader>no :NotesOpen<CR>
nnoremap <silent> <Leader>nt :NotesToggle<CR>
nnoremap <silent> <Leader>nn :NotesNew<CR>
nnoremap <silent> <Leader>nl :NotesList<CR>
nnoremap <silent> <Leader>nd :NotesDelete<CR>
nnoremap <silent> <Leader>ns :NotesSearch<CR>

