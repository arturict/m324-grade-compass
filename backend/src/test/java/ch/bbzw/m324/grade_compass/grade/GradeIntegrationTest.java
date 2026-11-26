package ch.bbzw.m324.grade_compass.grade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GradeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private GradeRepository repository;

    @BeforeEach
    void clean() { repository.deleteAll(); }

    private ResultActions postGrade(String json) throws Exception {
        return mockMvc.perform(post("/api/grades")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void createThenList_showsGrade() throws Exception {
        postGrade("{\"subjectId\":1,\"value\":5.0}").andExpect(status().isCreated());
        mockMvc.perform(get("/api/grades"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].value").value(5.0));
    }

    @Test
    void create_value7_returns400AndStoresNothing() throws Exception {
        postGrade("{\"subjectId\":1,\"value\":7}").andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void create_roundsBeforeSaving() throws Exception {
        postGrade("{\"subjectId\":1,\"value\":4.567}").andExpect(status().isCreated());
        assertThat(repository.findAll().get(0).getValue()).isEqualTo(4.57);
    }

    @Test
    void create_unknownSubject_isCurrentlyAccepted() throws Exception {
        postGrade("{\"subjectId\":999,\"value\":5.0}").andExpect(status().isCreated());
        assertThat(repository.count()).isEqualTo(1);
    }
}
