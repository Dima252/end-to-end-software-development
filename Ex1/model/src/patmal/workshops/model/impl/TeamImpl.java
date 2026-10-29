package patmal.workshops.model.impl;

import java.util.ArrayList;
import java.util.List;

import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;

public class TeamImpl implements Team {

    private final Integer id;
    private final String name;
    private final WorkshopImpl workshop;
    private final List<StudentImpl> students = new ArrayList<>();

    public TeamImpl(Integer id, String name, WorkshopImpl workshop) {
        this.id = id;
        this.name = name;
        this.workshop = workshop;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    /** Returns a copy, so callers cannot change the internal state. */
    @Override
    public List<Student> getStudents() {
        return new ArrayList<>(students);
    }

    @Override
    public Workshop getWorkshop() {
        return workshop;
    }

    /**
     * Updates both sides of the team-student relation. The caller must make sure the
     * student is free; this is validated by the system before anything is changed.
     */
    public void addStudent(StudentImpl student) {
        if (student.getTeam() != null) {
            throw new IllegalStateException("Student " + student.getId() + " is already in a team");
        }
        students.add(student);
        student.setTeam(this);
    }

    /** Internal access to the members, used to derive the team mates of a student. */
    List<StudentImpl> members() {
        return students;
    }

    @Override
    public String toString() {
        return id + " " + name;
    }
}
