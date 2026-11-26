package patmal.workshops.model.impl;

import java.util.Objects;

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
        if (Objects.equals(instructor, newInstructor)) {
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

    /**
     * Two workshops are equal when they have the same id, name and description. The instructor is
     * left out on purpose: it can change, and comparing it would recurse into the instructor's
     * workshops (a cycle).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkshopImpl other = (WorkshopImpl) o;
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return id + " " + name;
    }
}
