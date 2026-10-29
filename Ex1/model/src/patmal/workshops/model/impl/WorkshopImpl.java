package patmal.workshops.model.impl;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;

public class WorkshopImpl implements Workshop {

    private final Integer id;
    private final String name;
    private final String description;
    private InstructorImpl instructor;

    public WorkshopImpl(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /** null until the workshop is associated with an instructor. */
    @Override
    public Instructor getInstructor() {
        return instructor;
    }

    /**
     * Updates both sides of the instructor-workshop relation. If the workshop was taught by
     * another instructor, it is removed from that instructor's list.
     */
    public void setInstructor(InstructorImpl newInstructor) {
        if (instructor == newInstructor) {
            return;
        }
        if (instructor != null) {
            instructor.removeWorkshop(this);
        }
        instructor = newInstructor;
        if (newInstructor != null) {
            newInstructor.addWorkshop(this);
        }
    }

    @Override
    public String toString() {
        return id + " " + name;
    }
}
