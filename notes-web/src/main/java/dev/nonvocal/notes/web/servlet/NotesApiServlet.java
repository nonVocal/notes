package dev.nonvocal.notes.web.servlet;

import dev.nonvocal.notes.core.entity.Note;
import dev.nonvocal.notes.core.service.NoteService;
import dev.nonvocal.notes.core.service.NoteServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * REST-style servlet handling {@code /api/notes/*}.
 *
 * <ul>
 *   <li>GET    /api/notes      – list all notes</li>
 *   <li>GET    /api/notes/{id} – get note by id</li>
 *   <li>POST   /api/notes      – create note (JSON body)</li>
 *   <li>PUT    /api/notes/{id} – update note (JSON body)</li>
 *   <li>DELETE /api/notes/{id} – delete note</li>
 * </ul>
 */
@WebServlet(urlPatterns = "/api/notes/*", name = "NotesApiServlet")
public class NotesApiServlet extends HttpServlet {

    private NoteService noteService;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        noteService   = new NoteServiceImpl();
        objectMapper  = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // ------------------------------------------------------------------
    // GET
    // ------------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String pathInfo = req.getPathInfo(); // null or "/{id}"
        resp.setContentType("application/json;charset=UTF-8");

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/notes – return all notes
            objectMapper.writeValue(resp.getWriter(), noteService.getAllNotes());
        } else {
            Long id = parseId(pathInfo, resp);
            if (id == null) return;

            Note note = noteService.getNote(id);
            if (note == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Note with id " + id + " not found");
                return;
            }
            objectMapper.writeValue(resp.getWriter(), note);
        }
    }

    // ------------------------------------------------------------------
    // POST
    // ------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Note incoming = objectMapper.readValue(req.getInputStream(), Note.class);
        Note created  = noteService.createNote(incoming);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(resp.getWriter(), created);
    }

    // ------------------------------------------------------------------
    // PUT
    // ------------------------------------------------------------------
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Long id = parseId(req.getPathInfo(), resp);
        if (id == null) return;

        Note incoming = objectMapper.readValue(req.getInputStream(), Note.class);

        try {
            Note updated = noteService.updateNote(id, incoming);
            resp.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(resp.getWriter(), updated);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Long id = parseId(req.getPathInfo(), resp);
        if (id == null) return;

        noteService.deleteNote(id);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------
    private Long parseId(String pathInfo, HttpServletResponse resp) throws IOException {
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing note id");
            return null;
        }
        try {
            return Long.parseLong(pathInfo.substring(1)); // strip leading "/"
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid note id");
            return null;
        }
    }
}

