" =============================================================================
" autoload/notes.vim - Autoloaded functions for the Notes plugin
" =============================================================================

" Open a new buffer to compose a note
function! notes#new() abort
    let l:title = input('Note title: ')
    if empty(l:title)
        echo 'Aborted.'
        return
    endif

    execute g:notes_split . ' new'
    setlocal buftype=nofile bufhidden=wipe noswapfile
    setlocal filetype=markdown
    call append(0, ['# ' . l:title, ''])
    call cursor(2, 1)
    startinsert!
endfunction

" List all notes (fetched via curl from the REST API)
function! notes#list() abort
    let l:cmd = 'curl -s ' . shellescape(g:notes_api_url)
    let l:result = system(l:cmd)
    if v:shell_error
        echohl ErrorMsg | echo 'Failed to fetch notes: ' . l:result | echohl None
        return
    endif
    execute g:notes_split . ' new'
    setlocal buftype=nofile bufhidden=wipe noswapfile filetype=json
    call setline(1, split(l:result, "\n"))
endfunction

" Delete a note by id
function! notes#delete() abort
    let l:id = input('Note ID to delete: ')
    if empty(l:id)
        return
    endif
    let l:cmd = 'curl -s -X DELETE ' . shellescape(g:notes_api_url . '/' . l:id)
    call system(l:cmd)
    echo 'Note ' . l:id . ' deleted.'
endfunction

" Search notes by keyword
function! notes#search() abort
    let l:query = input('Search: ')
    if empty(l:query)
        return
    endif
    let l:cmd = 'curl -s ' . shellescape(g:notes_api_url . '?q=' . l:query)
    let l:result = system(l:cmd)
    execute g:notes_split . ' new'
    setlocal buftype=nofile bufhidden=wipe noswapfile filetype=json
    call setline(1, split(l:result, "\n"))
endfunction

