package patmal.workshops.student.impl.logic;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Person;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;

/**
 * Writes the three JSON exports. The model is first copied into small records that have exactly
 * the fields of each export, and Jackson writes those records. A record never refers back to an
 * ancestor, so the output is always a tree (no student -> team -> student cycle).
 *
 * <p>An export only reads the system, it never changes it.
 */
class JsonExporter {

    private static final Logger LOG = LogManager.getLogger(JsonExporter.class);

    /** Keeps the fields in record order (id, name, ...) instead of Jackson 3's alphabetical default. */
    private static final ObjectWriter WRITER = JsonMapper.builder()
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build()
            .writerWithDefaultPrettyPrinter();

    private final InstructorManager instructors;
    private final WorkshopManager workshops;
    private final StudentManager students;
    private final TeamManager teams;

    JsonExporter(InstructorManager instructors, WorkshopManager workshops,
                 StudentManager students, TeamManager teams) {
        this.instructors = instructors;
        this.workshops = workshops;
        this.students = students;
        this.teams = teams;
    }

    // ---------------------------------------------------------------- the three exports

    /** Instructor -> workshops -> teams -> student details. */
    void exportInstructors(File target) {
        List<InstructorJson> data = new ArrayList<>();
        for (Instructor instructor : instructors.getAll()) {
            List<WorkshopWithTeamsJson> taught = new ArrayList<>();
            for (Workshop workshop : workshopsOf(instructor)) {
                List<TeamWithStudentsJson> workshopTeams = new ArrayList<>();
                for (Team team : teamsOf(workshop)) {
                    workshopTeams.add(new TeamWithStudentsJson(team.getId(), team.getName(),
                            details(team.getStudents())));
                }
                taught.add(new WorkshopWithTeamsJson(workshop.getId(), workshop.getName(),
                        workshop.getDescription(), workshopTeams));
            }
            data.add(new InstructorJson(instructor.getId(), instructor.getName(), instructor.getEmail(), taught));
        }
        write(target, data, "instructors");
    }

    /** Student -> team (with the ids of its students) -> workshop -> instructor. */
    void exportStudents(File target) {
        Map<String, Team> teamOfStudent = new HashMap<>();
        for (Team team : teams.getAll()) {
            for (Student member : team.getStudents()) {
                teamOfStudent.put(member.getId(), team);
            }
        }

        List<StudentJson> data = new ArrayList<>();
        for (Student student : students.getAll()) {
            Team team = teamOfStudent.get(student.getId());
            StudentTeamJson teamJson = null;
            if (team != null) {
                List<String> studentIds = new ArrayList<>();
                for (Student member : team.getStudents()) {
                    studentIds.add(member.getId());
                }
                teamJson = new StudentTeamJson(team.getId(), team.getName(), studentIds,
                        withInstructor(team.getWorkshop()));
            }
            data.add(new StudentJson(student.getId(), student.getName(), student.getEmail(), teamJson));
        }
        write(target, data, "students");
    }

    /** Team -> student details, and team -> workshop -> instructor. */
    void exportTeams(File target) {
        List<TeamJson> data = new ArrayList<>();
        for (Team team : teams.getAll()) {
            data.add(new TeamJson(team.getId(), team.getName(), details(team.getStudents()),
                    withInstructor(team.getWorkshop())));
        }
        write(target, data, "teams");
    }

    // ---------------------------------------------------------------- helpers

    /** The instructor's workshops in creation order. */
    private List<Workshop> workshopsOf(Instructor instructor) {
        List<Workshop> result = new ArrayList<>();
        for (Workshop workshop : workshops.getAll()) {
            if (instructor.equals(workshop.getInstructor())) {
                result.add(workshop);
            }
        }
        return result;
    }

    /** The workshop's teams in creation order. */
    private List<Team> teamsOf(Workshop workshop) {
        List<Team> result = new ArrayList<>();
        for (Team team : teams.getAll()) {
            if (workshop.equals(team.getWorkshop())) {
                result.add(team);
            }
        }
        return result;
    }

    private static List<PersonJson> details(List<Student> members) {
        List<PersonJson> result = new ArrayList<>();
        for (Student member : members) {
            result.add(details(member));
        }
        return result;
    }

    /** Only id, name and e-mail; null stays null (a workshop without an instructor). */
    private static PersonJson details(Person person) {
        return person == null ? null : new PersonJson(person.getId(), person.getName(), person.getEmail());
    }

    private static WorkshopWithInstructorJson withInstructor(Workshop workshop) {
        return new WorkshopWithInstructorJson(workshop.getId(), workshop.getName(),
                workshop.getDescription(), details(workshop.getInstructor()));
    }

    /**
     * Jackson only builds the text; the file is written by java.nio, in UTF-8. An existing file
     * is overwritten, a missing file is created, and every I/O failure is an UncheckedIOException.
     */
    private static void write(File target, List<?> data, String what) {
        try {
            Validation.requireNonNull(target, "Target file");
            String json = WRITER.writeValueAsString(data);
            try {
                Files.writeString(target.toPath(), json, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot write " + target, e);
            }
            LOG.info("Exported {} {} to {}", data.size(), what, target.getAbsolutePath());
        } catch (IllegalArgumentException | UncheckedIOException e) {
            LOG.warn("Rejected export of {}: {}", what, e.getMessage());
            throw e;
        }
    }

    // ---------------------------------------------------------------- the shapes of the exports

    record PersonJson(String id, String name, String email) {
    }

    record InstructorJson(String id, String name, String email, List<WorkshopWithTeamsJson> workshops) {
    }

    record WorkshopWithTeamsJson(Integer id, String name, String description, List<TeamWithStudentsJson> teams) {
    }

    record TeamWithStudentsJson(Integer id, String name, List<PersonJson> students) {
    }

    record StudentJson(String id, String name, String email, StudentTeamJson team) {
    }

    record StudentTeamJson(Integer id, String name, List<String> studentIds, WorkshopWithInstructorJson workshop) {
    }

    record WorkshopWithInstructorJson(Integer id, String name, String description, PersonJson instructor) {
    }

    record TeamJson(Integer id, String name, List<PersonJson> students, WorkshopWithInstructorJson workshop) {
    }
}
