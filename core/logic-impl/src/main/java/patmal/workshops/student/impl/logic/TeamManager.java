package patmal.workshops.student.impl.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.model.StudentImpl;
import patmal.workshops.student.impl.model.TeamImpl;
import patmal.workshops.student.impl.model.WorkshopImpl;

/**
 * Creates, stores and looks up the teams of one system, and answers the questions that go
 * through teams (the students of a workshop or of an instructor).
 */
class TeamManager {

    private static final Logger LOG = LogManager.getLogger(TeamManager.class);

    private final InstructorManager instructors;
    private final WorkshopManager workshops;
    private final StudentManager students;
    private final Map<Integer, TeamImpl> teams = new LinkedHashMap<>();

    private int nextTeamId = 1;

    TeamManager(InstructorManager instructors, WorkshopManager workshops, StudentManager students) {
        this.instructors = instructors;
        this.workshops = workshops;
        this.students = students;
    }

    Team create(String name, Workshop workshop, List<String> members) {
        try {
            WorkshopImpl knownWorkshop = workshops.resolve(workshop);
            Validation.requireNonNull(members, "Team members");
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
                StudentImpl student = students.find(studentId);
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
            LOG.info("Created team {} '{}' in workshop {} with {} student(s)",
                    team.getId(), name, knownWorkshop.getId(), teamStudents.size());
            return team;
        } catch (IllegalArgumentException e) {
            LOG.warn("Rejected createTeam: {}", e.getMessage());
            throw e;
        }
    }

    List<Team> getAll() {
        return new ArrayList<>(teams.values());
    }

    /** Returns null for an unknown id. */
    Team get(Integer id) {
        return id == null ? null : teams.get(id);
    }

    /**
     * Returns an empty list for an unknown instructor id. The workshops are visited in creation
     * order (not in the order they were assigned to the instructor), like everywhere else.
     */
    List<Student> getStudentsOfInstructor(String instructorId) {
        Instructor instructor = instructors.get(instructorId);
        if (instructor == null) {
            return new ArrayList<>();
        }
        Set<Student> result = new LinkedHashSet<>();
        for (Workshop workshop : workshops.getAll()) {
            if (instructor.equals(workshop.getInstructor())) {
                result.addAll(getStudentsOfWorkshop(workshop.getId()));
            }
        }
        return new ArrayList<>(result);
    }

    /** Returns an empty list for an unknown workshop id. */
    List<Student> getStudentsOfWorkshop(Integer workshopId) {
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
}
