package patmal.workshops.student.impl.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import patmal.workshops.api.model.Student;
import patmal.workshops.student.impl.model.StudentImpl;

/** Creates, stores and looks up the students of one system. */
class StudentManager {

    private static final Logger LOG = LogManager.getLogger(StudentManager.class);

    private final Map<String, StudentImpl> students = new LinkedHashMap<>();

    Student create(String id, String name, String email) {
        try {
            Validation.requireNonNull(id, "Student id");
            if (students.containsKey(id)) {
                throw new IllegalArgumentException("A student with id '" + id + "' already exists");
            }
            StudentImpl student = new StudentImpl(id, name, email);
            students.put(id, student);
            LOG.info("Created student '{}' ({})", id, name);
            return student;
        } catch (IllegalArgumentException e) {
            LOG.warn("Rejected createStudent: {}", e.getMessage());
            throw e;
        }
    }

    List<Student> getAll() {
        return new ArrayList<>(students.values());
    }

    /** Returns null for an unknown id. */
    Student get(String id) {
        return find(id);
    }

    /** Our own object for the id, or null for an unknown id. Used by the team manager. */
    StudentImpl find(String id) {
        return id == null ? null : students.get(id);
    }

    List<Student> getFree() {
        List<Student> free = new ArrayList<>();
        for (StudentImpl student : students.values()) {
            if (student.isFree()) {
                free.add(student);
            }
        }
        return free;
    }
}
