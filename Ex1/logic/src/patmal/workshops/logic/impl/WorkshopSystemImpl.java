package patmal.workshops.logic.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.model.impl.InstructorImpl;
import patmal.workshops.model.impl.StudentImpl;
import patmal.workshops.model.impl.TeamImpl;
import patmal.workshops.model.impl.WorkshopImpl;

/**
 * In-memory implementation of {@link WorkshopSystem}.
 *
 * <p>Data is kept in maps keyed by id (insertion ordered, so lists come back in creation order).
 * Every create operation validates its whole request before changing anything, so a rejected
 * operation leaves the system, including the id counters, exactly as it was.
 *
 * <p>Invalid operations throw {@link IllegalArgumentException}.
 *
 * <p>Unknown keys: lookups with an id that does not exist do NOT throw. {@code getX(id)} returns
 * {@code null} and {@code getStudentsOfX(id)} returns an empty list.
 *
 * <p>All returned lists are new copies, never {@code null}, and never contain duplicates.
 */
public class WorkshopSystemImpl implements WorkshopSystem {

    private final Map<String, InstructorImpl> instructors = new LinkedHashMap<>();
    private final Map<String, StudentImpl> students = new LinkedHashMap<>();
    private final Map<Integer, WorkshopImpl> workshops = new LinkedHashMap<>();
    private final Map<Integer, TeamImpl> teams = new LinkedHashMap<>();

    private int nextWorkshopId = 1;
    private int nextTeamId = 1;

    public WorkshopSystemImpl() {
    }

    // ---------------------------------------------------------------- creating things

    @Override
    public Instructor createInstructor(String id, String name, String email) {
        requireNonNull(id, "Instructor id");
        if (instructors.containsKey(id)) {
            throw new IllegalArgumentException("An instructor with id '" + id + "' already exists");
        }
        InstructorImpl instructor = new InstructorImpl(id, name, email);
        instructors.put(id, instructor);
        return instructor;
    }

    @Override
    public Student createStudent(String id, String name, String email) {
        requireNonNull(id, "Student id");
        if (students.containsKey(id)) {
            throw new IllegalArgumentException("A student with id '" + id + "' already exists");
        }
        StudentImpl student = new StudentImpl(id, name, email);
        students.put(id, student);
        return student;
    }

    @Override
    public Workshop createWorkshop(String name, String description) {
        WorkshopImpl workshop = new WorkshopImpl(nextWorkshopId, name, description);
        workshops.put(workshop.getId(), workshop);
        nextWorkshopId++;
        return workshop;
    }

    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        InstructorImpl knownInstructor = resolveInstructor(instructor);
        WorkshopImpl knownWorkshop = resolveWorkshop(workshop);
        knownWorkshop.setInstructor(knownInstructor);
    }

    @Override
    public Team createTeam(String name, Workshop workshop, List<String> members) {
        WorkshopImpl knownWorkshop = resolveWorkshop(workshop);
        requireNonNull(members, "Team members");
        if (members.isEmpty()) {
            throw new IllegalArgumentException("A team must have at least one student");
        }

        // Validate all members before changing anything.
        List<StudentImpl> teamStudents = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String studentId : members) {
            if (!seen.add(studentId)) {
                throw new IllegalArgumentException("Student '" + studentId + "' is listed more than once");
            }
            StudentImpl student = students.get(studentId);
            if (student == null) {
                throw new IllegalArgumentException("Student '" + studentId + "' does not exist");
            }
            if (!student.isFree()) {
                throw new IllegalArgumentException(
                        "Student '" + studentId + "' is already in team '" + student.getTeamName() + "'");
            }
            teamStudents.add(student);
        }

        // Everything is valid - only now consume an id and apply the change.
        TeamImpl team = new TeamImpl(nextTeamId, name, knownWorkshop);
        for (StudentImpl student : teamStudents) {
            team.addStudent(student);
        }
        teams.put(team.getId(), team);
        nextTeamId++;
        return team;
    }

    // ---------------------------------------------------------------- querying

    @Override
    public List<Instructor> getInstructors() {
        return new ArrayList<>(instructors.values());
    }

    /** Returns null for an unknown id. */
    @Override
    public Instructor getInstructor(String id) {
        return id == null ? null : instructors.get(id);
    }

    @Override
    public List<Student> getStudents() {
        return new ArrayList<>(students.values());
    }

    /** Returns null for an unknown id. */
    @Override
    public Student getStudent(String id) {
        return id == null ? null : students.get(id);
    }

    @Override
    public List<Workshop> getWorkshops() {
        return new ArrayList<>(workshops.values());
    }

    /** Returns null for an unknown id. */
    @Override
    public Workshop getWorkshop(Integer id) {
        return id == null ? null : workshops.get(id);
    }

    @Override
    public List<Team> getTeams() {
        return new ArrayList<>(teams.values());
    }

    /** Returns null for an unknown id. */
    @Override
    public Team getTeam(Integer id) {
        return id == null ? null : teams.get(id);
    }

    @Override
    public List<Student> getFreeStudents() {
        List<Student> free = new ArrayList<>();
        for (StudentImpl student : students.values()) {
            if (student.isFree()) {
                free.add(student);
            }
        }
        return free;
    }

    /** Returns an empty list for an unknown instructor id. */
    @Override
    public List<Student> getStudentsOfInstructor(String instructorId) {
        Instructor instructor = getInstructor(instructorId);
        if (instructor == null) {
            return new ArrayList<>();
        }
        Set<Student> result = new LinkedHashSet<>();
        for (Workshop workshop : instructor.getWorkshops()) {
            result.addAll(getStudentsOfWorkshop(workshop.getId()));
        }
        return new ArrayList<>(result);
    }

    /** Returns an empty list for an unknown workshop id. */
    @Override
    public List<Student> getStudentsOfWorkshop(Integer workshopId) {
        Set<Student> result = new LinkedHashSet<>();
        if (workshopId == null) {
            return new ArrayList<>(result);
        }
        for (TeamImpl team : teams.values()) {
            if (workshopId.equals(team.getWorkshop().getId())) {
                result.addAll(team.getStudents());
            }
        }
        return new ArrayList<>(result);
    }

    // ---------------------------------------------------------------- helpers

    /** Makes sure the given instructor was created by this system and returns our own object. */
    private InstructorImpl resolveInstructor(Instructor instructor) {
        requireNonNull(instructor, "Instructor");
        InstructorImpl known = instructors.get(instructor.getId());
        if (known == null || known != instructor) {
            throw new IllegalArgumentException("Instructor '" + instructor.getId() + "' is not known to this system");
        }
        return known;
    }

    /** Makes sure the given workshop was created by this system and returns our own object. */
    private WorkshopImpl resolveWorkshop(Workshop workshop) {
        requireNonNull(workshop, "Workshop");
        WorkshopImpl known = workshops.get(workshop.getId());
        if (known == null || known != workshop) {
            throw new IllegalArgumentException("Workshop '" + workshop.getId() + "' is not known to this system");
        }
        return known;
    }

    private static void requireNonNull(Object value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }
    }
}
