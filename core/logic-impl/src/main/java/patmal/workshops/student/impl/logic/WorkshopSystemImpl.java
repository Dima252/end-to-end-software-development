package patmal.workshops.student.impl.logic;

import java.io.File;
import java.util.List;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;

/**
 * In-memory implementation of {@link WorkshopSystem}. It is a thin facade: every call is
 * delegated to one of the per-area managers (instructors, workshops, students, teams), to the
 * JSON exporter or to the CSV importer.
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

    private final InstructorManager instructors;
    private final WorkshopManager workshops;
    private final StudentManager students;
    private final TeamManager teams;
    private final JsonExporter json;
    private final CsvImporter csv;

    public WorkshopSystemImpl() {
        instructors = new InstructorManager();
        workshops = new WorkshopManager(instructors);
        students = new StudentManager();
        teams = new TeamManager(instructors, workshops, students);
        json = new JsonExporter(instructors, workshops, students, teams);
        csv = new CsvImporter(students);
    }

    // ---------------------------------------------------------------- creating things

    @Override
    public Instructor createInstructor(String id, String name, String email) {
        return instructors.create(id, name, email);
    }

    @Override
    public Student createStudent(String id, String name, String email) {
        return students.create(id, name, email);
    }

    @Override
    public Workshop createWorkshop(String name, String description) {
        return workshops.create(name, description);
    }

    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        workshops.associate(instructor, workshop);
    }

    @Override
    public Team createTeam(String name, Workshop workshop, List<String> members) {
        return teams.create(name, workshop, members);
    }

    // ---------------------------------------------------------------- querying

    @Override
    public List<Instructor> getInstructors() {
        return instructors.getAll();
    }

    @Override
    public Instructor getInstructor(String id) {
        return instructors.get(id);
    }

    @Override
    public List<Student> getStudents() {
        return students.getAll();
    }

    @Override
    public Student getStudent(String id) {
        return students.get(id);
    }

    @Override
    public List<Workshop> getWorkshops() {
        return workshops.getAll();
    }

    @Override
    public Workshop getWorkshop(Integer id) {
        return workshops.get(id);
    }

    @Override
    public List<Team> getTeams() {
        return teams.getAll();
    }

    @Override
    public Team getTeam(Integer id) {
        return teams.get(id);
    }

    @Override
    public List<Student> getFreeStudents() {
        return students.getFree();
    }

    @Override
    public List<Student> getStudentsOfInstructor(String instructorId) {
        return teams.getStudentsOfInstructor(instructorId);
    }

    @Override
    public List<Student> getStudentsOfWorkshop(Integer workshopId) {
        return teams.getStudentsOfWorkshop(workshopId);
    }

    // ---------------------------------------------------------------- export

    @Override
    public void exportInstructorsDataToJson(File targetFileName) {
        json.exportInstructors(targetFileName);
    }

    @Override
    public void exportStudentsDataToJson(File targetFileName) {
        json.exportStudents(targetFileName);
    }

    @Override
    public void exportTeamsDataToJson(File targetFileName) {
        json.exportTeams(targetFileName);
    }

    // ---------------------------------------------------------------- import

    @Override
    public List<Student> importStudentDataFromFile(File source) {
        return csv.importStudents(source);
    }
}
