package patmal.workshops.student.impl.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.logic.WorkshopSystemImpl;

/**
 * workshop-cli: fills a workshop system with demo data and exports it to
 * {@code instructors.json}, {@code students.json} and {@code teams.json} in the current folder.
 *
 * <p>The students are imported from a CSV file: the file given as the only argument, or else the
 * {@code students.csv} packed in the jar.
 *
 * <pre>java -jar workshop-cli.jar [students.csv]</pre>
 */
public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    private static final String PACKED_STUDENTS = "/students.csv";

    /** Two students in one team, one in another, and at least one free student. */
    private static final int MIN_STUDENTS = 4;

    public static void main(String[] args) {
        try {
            run(args);
        } catch (RuntimeException e) {
            LOG.error("workshop-cli failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: java -jar workshop-cli.jar [students.csv]");
        }
        LOG.info("workshop-cli started");

        WorkshopSystem system = new WorkshopSystemImpl();
        List<Student> students = args.length == 1
                ? importStudents(system, new File(args[0]))
                : importPackedStudents(system);
        createDemoData(system, students);
        LOG.info("Demo data: {} instructors, {} workshops, {} students ({} free), {} teams",
                system.getInstructors().size(), system.getWorkshops().size(),
                system.getStudents().size(), system.getFreeStudents().size(),
                system.getTeams().size());

        File instructorsFile = new File("instructors.json");
        system.exportInstructorsDataToJson(instructorsFile);
        LOG.info("Instructors written to {}", instructorsFile.getAbsolutePath());

        File studentsFile = new File("students.json");
        system.exportStudentsDataToJson(studentsFile);
        LOG.info("Students written to {}", studentsFile.getAbsolutePath());

        File teamsFile = new File("teams.json");
        system.exportTeamsDataToJson(teamsFile);
        LOG.info("Teams written to {}", teamsFile.getAbsolutePath());

        LOG.info("workshop-cli finished");
    }

    private static List<Student> importStudents(WorkshopSystem system, File csv) {
        LOG.info("Reading students from {}", csv.getAbsolutePath());
        return system.importStudentDataFromFile(csv);
    }

    /**
     * The import reads a {@link File}, and a resource inside a jar is not one: the packed
     * students.csv is copied to a temporary file first, and that file is deleted afterwards.
     */
    private static List<Student> importPackedStudents(WorkshopSystem system) {
        LOG.info("No CSV file given, using the students.csv packed in workshop-cli.jar");
        Path copy = null;
        try (InputStream packed = Main.class.getResourceAsStream(PACKED_STUDENTS)) {
            if (packed == null) {
                throw new IllegalStateException(PACKED_STUDENTS + " is missing from the jar");
            }
            copy = Files.createTempFile("workshop-cli-students", ".csv");
            Files.copy(packed, copy, StandardCopyOption.REPLACE_EXISTING);
            return system.importStudentDataFromFile(copy.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the packed " + PACKED_STUDENTS, e);
        } finally {
            deleteQuietly(copy);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.warn("Cannot delete the temporary file {}", file);
        }
    }

    /**
     * Two instructors and three workshops. The teams are formed from the imported students by
     * position: the first two are team Alpha, the third is team Beta, the others stay free. The
     * third workshop has no team.
     */
    private static void createDemoData(WorkshopSystem system, List<Student> students) {
        if (students.size() < MIN_STUDENTS) {
            throw new IllegalArgumentException("The CSV file must list at least " + MIN_STUDENTS
                    + " students, found " + students.size());
        }

        Instructor dana = system.createInstructor("i1", "Dana Levi", "dana.levi@univ.edu");
        Instructor yossi = system.createInstructor("i2", "Yossi Mizrahi", "yossi.mizrahi@univ.edu");

        Workshop java = system.createWorkshop("Java Workshop", "Plain Java from the ground up");
        Workshop maven = system.createWorkshop("Maven Workshop", "Multi-module builds and uber jars");
        Workshop spring = system.createWorkshop("Spring Workshop", "Dependency injection with Spring");
        system.associateWorkshopWithInstructor(dana, java);
        system.associateWorkshopWithInstructor(dana, maven);
        system.associateWorkshopWithInstructor(yossi, spring);

        system.createTeam("Alpha", java, List.of(students.get(0).getId(), students.get(1).getId()));
        system.createTeam("Beta", maven, List.of(students.get(2).getId()));
    }
}
