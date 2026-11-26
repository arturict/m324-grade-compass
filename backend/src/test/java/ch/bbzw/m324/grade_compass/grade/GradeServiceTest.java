package ch.bbzw.m324.grade_compass.grade;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository repository;

    @InjectMocks
    private GradeService service;

    @Test
    void create_savesGradeAndReturnsResponse() {
        when(repository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeResponse response = service.create(new CreateGradeRequest(1L, 5.5));

        assertThat(response.subjectId()).isEqualTo(1L);
        assertThat(response.value()).isEqualTo(5.5);
        verify(repository).save(any(Grade.class));
    }

    @Test
    void create_valueOutOfRange_throwsBadRequestAndDoesNotSave() {
        assertThatThrownBy(() -> service.create(new CreateGradeRequest(1L, 7.0)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(repository, never()).save(any());
    }

    @Test
    void findAll_mapsEntitiesToResponses() {
        when(repository.findAll()).thenReturn(List.of(new Grade(1L, 4.0), new Grade(2L, 5.5)));

        List<GradeResponse> result = service.findAll();

        assertThat(result).extracting(GradeResponse::value).containsExactly(4.0, 5.5);
    }

    @Test
    void findAll_returnsEmptyListWhenNoGradesExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 6.0})
    void create_boundaryValues_areAccepted(double value) {
        when(repository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(new CreateGradeRequest(1L, value)).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.99, 6.01})
    void create_valuesOutsideBoundary_areRejected(double value) {
        assertThatThrownBy(() -> service.create(new CreateGradeRequest(1L, value)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repository, never()).save(any());
    }

    @Test
    void create_roundsToTwoDecimals() {
        when(repository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeResponse response = service.create(new CreateGradeRequest(1L, 4.567));

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo(4.57);
        assertThat(response.value()).isEqualTo(4.57);
    }

    @Test
    void create_saveFails_propagatesException() {
        when(repository.save(any(Grade.class))).thenThrow(new RuntimeException("DB nicht erreichbar"));
        
        assertThatThrownBy(() -> service.create(new CreateGradeRequest(1L, 5.0)))
                .hasMessage("DB nicht erreichbar");
    }
}
