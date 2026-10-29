package patmal.workshops.model.impl;

import java.util.ArrayList;
import java.util.List;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;

public class InstructorImpl extends PersonImpl implements Instructor {

    private final List<WorkshopImpl> workshops = new ArrayList<>();

    public InstructorImpl(String id, String name, String email) {
        super(id, name, email);
    }

    /** Returns a copy, so callers cannot change the internal state. */
    @Override
    public List<Workshop> getWorkshops() {
        return new ArrayList<>(workshops);
    }

    /**
     * Only one side of the relation. Use {@link WorkshopImpl#setInstructor} to keep both sides in sync.
     */
    void addWorkshop(WorkshopImpl workshop) {
        if (!workshops.contains(workshop)) {
            workshops.add(workshop);
        }
    }

    void removeWorkshop(WorkshopImpl workshop) {
        workshops.remove(workshop);
    }
}
