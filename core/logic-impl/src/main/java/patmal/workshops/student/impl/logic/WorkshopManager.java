package patmal.workshops.student.impl.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.model.InstructorImpl;
import patmal.workshops.student.impl.model.WorkshopImpl;

/** Creates, stores and looks up the workshops of one system, and links them to instructors. */
class WorkshopManager {

    private final InstructorManager instructors;
    private final Map<Integer, WorkshopImpl> workshops = new LinkedHashMap<>();

    private int nextWorkshopId = 1;

    WorkshopManager(InstructorManager instructors) {
        this.instructors = instructors;
    }

    Workshop create(String name, String description) {
        WorkshopImpl workshop = new WorkshopImpl(nextWorkshopId, name, description);
        workshops.put(workshop.getId(), workshop);
        nextWorkshopId++;
        return workshop;
    }

    void associate(Instructor instructor, Workshop workshop) {
        InstructorImpl knownInstructor = instructors.resolve(instructor);
        WorkshopImpl knownWorkshop = resolve(workshop);
        knownWorkshop.setInstructor(knownInstructor);
    }

    List<Workshop> getAll() {
        return new ArrayList<>(workshops.values());
    }

    /** Returns null for an unknown id. */
    Workshop get(Integer id) {
        return id == null ? null : workshops.get(id);
    }

    /** Makes sure the given workshop was created by this system and returns our own object. */
    WorkshopImpl resolve(Workshop workshop) {
        Validation.requireNonNull(workshop, "Workshop");
        WorkshopImpl known = workshops.get(workshop.getId());
        if (known == null || known != workshop) {
            throw new IllegalArgumentException("Workshop '" + workshop.getId() + "' is not known to this system");
        }
        return known;
    }
}
