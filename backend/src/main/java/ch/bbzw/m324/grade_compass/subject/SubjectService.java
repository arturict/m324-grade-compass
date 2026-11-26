package ch.bbzw.m324.grade_compass.subject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Geschäftslogik für Fächer: anlegen, auflisten, umbenennen und löschen.
 * Prüft doppelte Namen (ohne Gross-/Kleinschreibung) und unbekannte IDs.
 */
@Service
public class SubjectService {

    private final SubjectRepository repository;

    /**
     * Erstellt den Service.
     *
     * @param repository Zugriff auf die Tabelle der Fächer
     */
    public SubjectService(SubjectRepository repository) {
        this.repository = repository;
    }

    /**
     * Legt ein neues Fach an. Der Name wird vorher getrimmt.
     *
     * @param request Name des neuen Fachs
     * @return das gespeicherte Fach mit ID
     * @throws ResponseStatusException 409, wenn der Name bereits existiert
     */
    public SubjectResponse create(CreateSubjectRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists: " + name);
        }
        return SubjectResponse.from(repository.save(new Subject(name)));
    }

    /**
     * Liefert alle Fächer.
     *
     * @return alle Fächer, leer wenn keine existieren
     */
    public List<SubjectResponse> findAll() {
        return repository.findAll().stream().map(SubjectResponse::from).toList();
    }

    /**
     * Benennt ein bestehendes Fach um. Der Name wird vorher getrimmt.
     *
     * @param id ID des Fachs
     * @param request neuer Name
     * @return das geänderte Fach
     * @throws ResponseStatusException 404, wenn die ID nicht existiert; 409, wenn ein anderes Fach den Namen schon hat
     */
    @Transactional
    public SubjectResponse update(Long id, CreateSubjectRequest request) {
        Subject subject = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found: " + id));
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject already exists: " + name);
        }
        subject.setName(name);
        return SubjectResponse.from(repository.save(subject));
    }

    /**
     * Löscht ein Fach.
     *
     * @param id ID des Fachs
     * @throws ResponseStatusException 404, wenn die ID nicht existiert
     */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found: " + id);
        }
        repository.deleteById(id);
    }
}
