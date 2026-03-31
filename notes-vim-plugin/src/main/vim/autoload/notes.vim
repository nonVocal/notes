" =============================================================================
" autoload/notes.vim – Autoloaded functions for the Notes plugin
"
" Layout (mirrors the TUI):
"
"   ┌─ Notes ──────────┬──────────────────────────────────┐
"   │  All Notes       │                                  │
"   │  New Note        │   content window                 │
"   │  Search          │   (notes list / editor / search) │
"   │──────────────────│                                  │
"   │  q Close  ? Help │                                  │
"   └──────────────────┴──────────────────────────────────┘
"
" The sidebar is a non-file buffer in a fixed left split.
" Press <CR> or double-click an item to activate it.
" =============================================================================

" --------------------------------------------------------------------------
" Sidebar metadata
" --------------------------------------------------------------------------
let s:sidebar_bufname = 'Notes'
let s:sidebar_header_lines = 2   " '  Notes' + separator

" Navigation items: label shown in sidebar + function to call
let s:nav_items = [
    \ { 'label': '  All Notes',  'fn': 'notes#cmd_list'   },
    \ { 'label': '  New Note',   'fn': 'notes#cmd_new'    },
    \ { 'label': '  Search',     'fn': 'notes#cmd_search' },
    \ ]

" --------------------------------------------------------------------------
" Sidebar – open / close / toggle
" --------------------------------------------------------------------------

" Open the sidebar (or focus it if already open).
function! notes#open_sidebar() abort
    let l:winnr = bufwinnr(s:sidebar_bufname)
    if l:winnr != -1
        execute l:winnr . 'wincmd w'
        return
    endif

    " Create a fixed-width vertical split anchored to the far left.
    execute 'topleft ' . g:notes_sidebar_width . 'vsplit'
    enew

    setlocal buftype=nofile bufhidden=wipe noswapfile
    setlocal nobuflisted nonumber norelativenumber nofoldenable
    setlocal nowrap cursorline winfixwidth
    execute 'silent! file ' . s:sidebar_bufname

    call notes#render_sidebar()

    " ── Buffer-local mappings ────────────────────────────────────────────
    " Enter / double-click → activate item (mirrors TUI button press)
    nnoremap <buffer><silent> <CR>          :call notes#sidebar_action()<CR>
    nnoremap <buffer><silent> <2-LeftMouse> :call notes#sidebar_action()<CR>
    " q → close sidebar  (like TUI Exit button)
    nnoremap <buffer><silent> q             :call notes#close_sidebar()<CR>
    " Ctrl+F → search  (mirrors TUI global Ctrl+F shortcut, sidebar-local only)
    nnoremap <buffer><silent> <C-f>         :call notes#cmd_search()<CR>
    " ? → quick help
    nnoremap <buffer><silent> ?             :call notes#sidebar_help()<CR>

    " Leave focus in the content window so the user can start working
    wincmd l
endfunction

" Close the sidebar window (buffer is wiped automatically via bufhidden=wipe).
function! notes#close_sidebar() abort
    let l:winnr = bufwinnr(s:sidebar_bufname)
    if l:winnr != -1
        execute l:winnr . 'wincmd c'
    endif
endfunction

" Toggle the sidebar open / closed.
function! notes#toggle_sidebar() abort
    if bufwinnr(s:sidebar_bufname) != -1
        call notes#close_sidebar()
    else
        call notes#open_sidebar()
    endif
endfunction

" --------------------------------------------------------------------------
" Sidebar – render
" --------------------------------------------------------------------------

function! notes#render_sidebar() abort
    let l:winnr = bufwinnr(s:sidebar_bufname)
    if l:winnr == -1 | return | endif
    execute l:winnr . 'wincmd w'

    let l:w = g:notes_sidebar_width - 2   " inner width (no border)

    setlocal modifiable
    silent %delete _

    " Header
    call setline(1, [
        \ '  Notes',
        \ repeat('─', l:w),
        \ ])

    " Navigation items (one line each)
    for l:item in s:nav_items
        call append('$', l:item.label)
    endfor

    " Footer separator + hint
    call append('$', [
        \ repeat('─', l:w),
        \ '  q Close   ? Help',
        \ ])

    setlocal nomodifiable
    " Place cursor on the first nav item
    call cursor(s:sidebar_header_lines + 1, 1)
endfunction

" --------------------------------------------------------------------------
" Sidebar – action dispatch
" --------------------------------------------------------------------------

" Called when the user presses <CR> or double-clicks in the sidebar.
" Maps the cursor line to a nav item and invokes its function.
function! notes#sidebar_action() abort
    let l:idx = line('.') - s:sidebar_header_lines - 1
    if l:idx >= 0 && l:idx < len(s:nav_items)
        call call(s:nav_items[l:idx].fn, [])
    endif
endfunction

function! notes#sidebar_help() abort
    echo 'Notes  <CR>/<2-LeftMouse>: open item   q: close   <C-f>: search   ?: help'
endfunction

" --------------------------------------------------------------------------
" Content-window helper
" --------------------------------------------------------------------------

" Switch focus to the first window that is NOT the sidebar.
" If none exists, split one to the right.
function! s:content_win() abort
    let l:sidebar_nr = bufwinnr(s:sidebar_bufname)
    for l:i in range(1, winnr('$'))
        if l:i != l:sidebar_nr
            execute l:i . 'wincmd w'
            return
        endif
    endfor
    " Only the sidebar is open – open a content window beside it
    wincmd l
endfunction

" Open a scratch buffer in the content window with the given name and lines.
function! s:open_content(bufname, lines, ft) abort
    call s:content_win()
    enew
    setlocal buftype=nofile bufhidden=wipe noswapfile
    execute 'setlocal filetype=' . a:ft
    execute 'silent! file ' . fnameescape(a:bufname)
    call setline(1, a:lines)
    setlocal nomodifiable
endfunction

" --------------------------------------------------------------------------
" Commands  (invoked from sidebar actions, key maps, and :NotesXxx)
" --------------------------------------------------------------------------

" List all notes – fetched from the REST API via curl.
function! notes#cmd_list() abort
    let l:result = s:curl_get(g:notes_api_url)
    if l:result isnot v:null
        call s:open_content('Notes – All Notes', split(l:result, "\n"), 'json')
    endif
endfunction

" Open a new markdown buffer to compose a note.
function! notes#cmd_new() abort
    let l:title = input('Note title: ')
    if empty(l:title) | echo 'Aborted.' | return | endif

    call s:content_win()
    enew
    setlocal buftype=nofile bufhidden=wipe noswapfile
    setlocal filetype=markdown
    execute 'silent! file Notes\ –\ ' . fnameescape(l:title)
    call setline(1, ['# ' . l:title, ''])
    call cursor(2, 1)
    startinsert!
endfunction

" Search notes by keyword (mirrors TUI Ctrl+F / Search view).
function! notes#cmd_search() abort
    let l:query = input('Search: ')
    if empty(l:query) | return | endif

    let l:url    = g:notes_api_url . '?q=' . l:query
    let l:result = s:curl_get(l:url)
    if l:result isnot v:null
        call s:open_content(
            \ 'Notes – Search: ' . l:query,
            \ split(l:result, "\n"),
            \ 'json')
    endif
endfunction

" Delete a note by ID.
function! notes#cmd_delete() abort
    let l:id = input('Note ID to delete: ')
    if empty(l:id) | return | endif

    call system('curl -s -X DELETE ' . shellescape(g:notes_api_url . '/' . l:id))
    if v:shell_error
        echohl ErrorMsg | echo 'Delete failed.' | echohl None
    else
        echo 'Note ' . l:id . ' deleted.'
    endif
endfunction

" --------------------------------------------------------------------------
" HTTP helper
" --------------------------------------------------------------------------

function! s:curl_get(url) abort
    let l:result = system('curl -s ' . shellescape(a:url))
    if v:shell_error
        echohl ErrorMsg | echo 'Request failed: ' . l:result | echohl None
        return v:null
    endif
    return l:result
endfunction

" --------------------------------------------------------------------------
" Legacy aliases (keep old call sites working)
" --------------------------------------------------------------------------
function! notes#new()    abort | call notes#cmd_new()    | endfunction
function! notes#list()   abort | call notes#cmd_list()   | endfunction
function! notes#delete() abort | call notes#cmd_delete() | endfunction
function! notes#search() abort | call notes#cmd_search() | endfunction


