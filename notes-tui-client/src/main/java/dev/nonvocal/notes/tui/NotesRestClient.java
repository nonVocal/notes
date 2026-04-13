package dev.nonvocal.notes.tui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.nonvocal.notes.core.entity.Note;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * HTTP client for the Notes REST API provided by {@code notes-web}.
 *
 * <p>Supported endpoints:
 * <ul>
 *   <li>GET    {base}       – list all notes</li>
 *   <li>GET    {base}/{id}  – get note by id</li>
 *   <li>POST   {base}       – create note</li>
 *   <li>PUT    {base}/{id}  – update note</li>
 *   <li>DELETE {base}/{id}  – delete note</li>
 * </ul>
 *
 * <p>Default base URL: {@code http://localhost:8080/notes/api/notes}
 */
public class NotesRestClient {

    /** Default API base URL (matches notes-web Jetty context path). */
    public static final String DEFAULT_BASE_URL = "http://localhost:8080/notes/api/notes";

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public NotesRestClient() {
        this(DEFAULT_BASE_URL);
    }

    public NotesRestClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http    = HttpClient.newHttpClient();
        this.mapper  = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── GET /api/notes ────────────────────────────────────────────────────────

    public List<Note> getAllNotes() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return mapper.readValue(resp.body(), new TypeReference<List<Note>>() {});
    }

    // ── GET /api/notes/{id} ───────────────────────────────────────────────────

    public Note getNote(long id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + id))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return mapper.readValue(resp.body(), Note.class);
    }

    // ── POST /api/notes ───────────────────────────────────────────────────────

    public Note createNote(Note note) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(note);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return mapper.readValue(resp.body(), Note.class);
    }

    // ── PUT /api/notes/{id} ───────────────────────────────────────────────────

    public Note updateNote(long id, Note note) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(note);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + id))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return mapper.readValue(resp.body(), Note.class);
    }

    // ── DELETE /api/notes/{id} ────────────────────────────────────────────────

    public void deleteNote(long id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + id))
                .DELETE()
                .build();
        HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
        checkStatus(resp);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> void checkStatus(HttpResponse<T> resp) throws IOException {
        int status = resp.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " from " + resp.uri());
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}

