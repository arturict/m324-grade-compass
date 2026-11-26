package ch.bbzw.m324.grade_compass.grade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GradeController.class)
class GradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradeService service;

    @Test
    void post_validBody_returns201AndGrade() throws Exception {
        when(service.create(any(CreateGradeRequest.class)))
                .thenReturn(new GradeResponse(1L, 2L, 5.5));

        mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":2,\"value\":5.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.value").value(5.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"value\":5.5}", "{\"subjectId\":2}"})
    void post_missingField_returns400(String body) throws Exception {
        mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void get_returnsAllGrades() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new GradeResponse(1L, 2L, 4.0), new GradeResponse(2L, 2L, 5.5)));

        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
