package ch.bbzw.m324.grade_compass.subject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration Test gegen ein echtes PostgreSQL 18 im Docker-Container (Testcontainers).
 * Der Container startet vor den Tests und wird danach wieder entfernt.
 * {@code @ServiceConnection} ersetzt die H2-Verbindung aus src/test/resources durch die des Containers.
 */
@SpringBootTest
@Testcontainers
class SubjectPostgresTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private SubjectRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void saveAndRead_returnsStoredSubject() {
        Subject saved = repository.saveAndFlush(new Subject("Mathe"));

        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(Subject::getName)
                .isEqualTo("Mathe");
    }

    @Test
    void save_duplicateName_isRejectedByDatabase() {
        repository.saveAndFlush(new Subject("Mathe"));
        Subject duplicate = new Subject("Mathe");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void connectsToPostgres() {
        assertThat(postgres.getJdbcUrl()).startsWith("jdbc:postgresql://");
    }
}
