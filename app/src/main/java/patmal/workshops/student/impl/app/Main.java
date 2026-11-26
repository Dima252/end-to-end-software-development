package patmal.workshops.student.impl.app;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.logic.WorkshopSystemImpl;

/**
 * workshop-cli: fills a workshop system with demo data and exports it to
 * {@code instructors.json}, {@code students.json} and {@code teams.json} in the current folder.
 */
public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            run();
        } catch (RuntimeException e) {
            LOG.error("workshop-cli failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void run() {
        LOG.info("workshop-cli started");

        WorkshopSystem system = new WorkshopSystemImpl();
        createDemoData(system);
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

    /**
     * Two instructors and three workshops; five students, three of them in two teams, so two
     * students stay free and one workshop has no team.
     */
    private static void createDemoData(WorkshopSystem system) {
        Instructor dana = system.createInstructor("i1", "Dana Levi", "dana.levi@univ.edu");
        Instructor yossi = system.createInstructor("i2", "Yossi Mizrahi", "yossi.mizrahi@univ.edu");

        Workshop java = system.createWorkshop("Java Workshop", "Plain Java from the ground up");
        Workshop maven = system.createWorkshop("Maven Workshop", "Multi-module builds and uber jars");
        Workshop spring = system.createWorkshop("Spring Workshop", "Dependency injection with Spring");
        system.associateWorkshopWithInstructor(dana, java);
        system.associateWorkshopWithInstructor(dana, maven);
        system.associateWorkshopWithInstructor(yossi, spring);

        system.createStudent("s1", "Alice Cohen", "alice@students.edu");
        system.createStudent("s2", "Bob Friedman", "bob@students.edu");
        system.createStudent("s3", "Zoë Katz", "zoe@students.edu");
        system.createStudent("s4", "David Peretz", "david@students.edu");
        system.createStudent("s5", "Noa Shapiro", "noa@students.edu");

        system.createTeam("Alpha", java, List.of("s1", "s2"));
        system.createTeam("Beta", maven, List.of("s3"));
    }
}
