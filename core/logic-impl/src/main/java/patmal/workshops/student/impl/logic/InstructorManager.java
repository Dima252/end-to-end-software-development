package patmal.workshops.student.impl.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.student.impl.model.InstructorImpl;

/** Creates, stores and looks up the instructors of one system. */
class InstructorManager {

    private final Map<String, InstructorImpl> instructors = new LinkedHashMap<>();

    Instructor create(String id, String name, String email) {
        Validation.requireNonNull(id, "Instructor id");
        if (instructors.containsKey(id)) {
            throw new IllegalArgumentException("An instructor with id '" + id + "' already exists");
        }
        InstructorImpl instructor = new InstructorImpl(id, name, email);
        instructors.put(id, instructor);
        return instructor;
    }

    List<Instructor> getAll() {
        return new ArrayList<>(instructors.values());
    }

    /** Returns null for an unknown id. */
    Instructor get(String id) {
        return id == null ? null : instructors.get(id);
    }

    /** Makes sure the given instructor was created by this system and returns our own object. */
    InstructorImpl resolve(Instructor instructor) {
        Validation.requireNonNull(instructor, "Instructor");
        InstructorImpl known = instructors.get(instructor.getId());
        if (known == null || known != instructor) {
            throw new IllegalArgumentException("Instructor '" + instructor.getId() + "' is not known to this system");
        }
        return known;
    }
}
