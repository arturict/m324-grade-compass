package ch.bbzw.m324.grade_compass.subject;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectService service;

    @Test
    void post_validBody_returns201AndCreatedSubject() throws Exception {
        when(service.create(any(CreateSubjectRequest.class)))
                .thenReturn(new SubjectResponse(1L, "Mathe"));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mathe"));

        verify(service).create(new CreateSubjectRequest("Mathe"));
    }

    @Test
    void post_blankName_returns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void post_emptyName_returns400() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void post_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void post_nameLongerThan50Characters_returns400() throws Exception {
        String body = "{\"name\":\"" + "a".repeat(51) + "\"}";

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void post_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void post_duplicateName_returns409() throws Exception {
        when(service.create(any(CreateSubjectRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists: Mathe"));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void get_returnsAllSubjects() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new SubjectResponse(1L, "Mathe"),
                new SubjectResponse(2L, "Deutsch")));

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Mathe"))
                .andExpect(jsonPath("$[1].name").value("Deutsch"));
    }

    @Test
    void get_withoutSubjects_returnsEmptyArray() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void put_existing_returns200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(new SubjectResponse(1L, "Mathe"));
        mockMvc.perform(put("/api/subjects/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mathe"));
    }

    @Test
    void put_unknownId_returns404() throws Exception {
        when(service.update(eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mockMvc.perform(put("/api/subjects/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_blankName_returns400() throws Exception {
        mockMvc.perform(put("/api/subjects/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/subjects/1")).andExpect(status().isNoContent());
        verify(service).delete(1L);
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(service).delete(99L);
        mockMvc.perform(delete("/api/subjects/99")).andExpect(status().isNotFound());
    }
}
