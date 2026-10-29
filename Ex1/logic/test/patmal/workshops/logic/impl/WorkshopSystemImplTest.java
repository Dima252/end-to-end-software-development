package patmal.workshops.logic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Person;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;

/**
 * Small self-written tests (plain Java, no framework). They complement the tester and focus on
 * cases it does not cover. Run with: build.cmd (or build.cmd notest to skip both test runs).
 * Not packaged into the artifacts jars.
 */
public class WorkshopSystemImplTest {

    private static int passed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        test("ids are sequential from 1 and kept per kind", WorkshopSystemImplTest::sequentialIds);
        test("free student has null / empty derived data", WorkshopSystemImplTest::freeStudent);
        test("team mates exclude the student", WorkshopSystemImplTest::teamMatesExcludeSelf);
        test("rejected createTeam is atomic and consumes no id", WorkshopSystemImplTest::rejectedTeamIsAtomic);
        test("duplicate ids are rejected per kind only", WorkshopSystemImplTest::duplicateIds);
        test("empty and null member lists are rejected", WorkshopSystemImplTest::emptyTeamRejected);
        test("re-associating a workshop updates both instructors", WorkshopSystemImplTest::reassociateWorkshop);
        test("associating twice does not duplicate the workshop", WorkshopSystemImplTest::associateTwice);
        test("objects from another system are rejected", WorkshopSystemImplTest::foreignObjectsRejected);
        test("returned lists are copies", WorkshopSystemImplTest::returnedListsAreCopies);
        test("unknown keys return null / empty list", WorkshopSystemImplTest::unknownKeys);
        test("student instructor follows a later association", WorkshopSystemImplTest::instructorDerivedLive);

        System.out.println();
        System.out.println("Own tests: " + passed + " passed, " + failures.size() + " failed");
        failures.forEach(f -> System.out.println("  - " + f));
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    // ---------------------------------------------------------------- tests

    private static void sequentialIds() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Workshop w1 = system.createWorkshop("A", "a");
        Workshop w2 = system.createWorkshop("B", "b");
        system.createStudent("s1", "S1", "s1@x");
        system.createStudent("s2", "S2", "s2@x");
        Team t1 = system.createTeam("T1", w2, List.of("s1"));
        Team t2 = system.createTeam("T2", w1, List.of("s2"));
        equal(1, w1.getId(), "first workshop id");
        equal(2, w2.getId(), "second workshop id");
        equal(1, t1.getId(), "first team id");
        equal(2, t2.getId(), "second team id");
    }

    private static void freeStudent() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Student s = system.createStudent("s1", "S1", "s1@x");
        equal(null, s.getTeamName(), "team name");
        equal(null, s.getInstructor(), "instructor");
        equal(List.of(), s.getTeamMates(), "team mates");
        equal(List.of("s1"), ids(system.getFreeStudents()), "free students");
    }

    private static void teamMatesExcludeSelf() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Workshop w = system.createWorkshop("A", "a");
        for (String id : List.of("s1", "s2", "s3")) {
            system.createStudent(id, id, id + "@x");
        }
        system.createTeam("T", w, List.of("s1", "s2", "s3"));
        equal(List.of("s2", "s3"), ids(system.getStudent("s1").getTeamMates()), "s1 mates");
        equal(List.of("s1", "s3"), ids(system.getStudent("s2").getTeamMates()), "s2 mates");
        equal("T", system.getStudent("s3").getTeamName(), "s3 team name");
    }

    private static void rejectedTeamIsAtomic() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Workshop w = system.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@x");
        system.createStudent("s2", "S2", "s2@x");
        system.createStudent("s3", "S3", "s3@x");
        system.createTeam("Busy", w, List.of("s3"));

        throwsIllegalArgument(() -> system.createTeam("X", w, List.of("s1", "s99")), "unknown student");
        throwsIllegalArgument(() -> system.createTeam("X", w, List.of("s1", "s3")), "student already in a team");
        throwsIllegalArgument(() -> system.createTeam("X", w, List.of("s1", "s2", "s1")), "same student twice");

        equal(List.of("s1", "s2"), ids(system.getFreeStudents()), "s1, s2 still free");
        equal(1, system.getTeams().size(), "still one team");
        equal(List.of("s3"), ids(system.getTeam(1).getStudents()), "existing team untouched");
        equal(2, system.createTeam("Ok", w, List.of("s1", "s2")).getId(), "next team still gets id 2");
    }

    private static void duplicateIds() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        system.createStudent("x1", "Student", "s@x");
        system.createInstructor("x1", "Instructor", "i@x");
        throwsIllegalArgument(() -> system.createStudent("x1", "Other", "o@x"), "duplicate student");
        throwsIllegalArgument(() -> system.createInstructor("x1", "Other", "o@x"), "duplicate instructor");
        equal("Student", system.getStudent("x1").getName(), "original student kept");
        equal("Instructor", system.getInstructor("x1").getName(), "original instructor kept");
    }

    private static void emptyTeamRejected() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Workshop w = system.createWorkshop("A", "a");
        throwsIllegalArgument(() -> system.createTeam("Empty", w, List.of()), "empty members");
        throwsIllegalArgument(() -> system.createTeam("Null", w, null), "null members");
        equal(0, system.getTeams().size(), "no team created");
    }

    private static void reassociateWorkshop() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Instructor i1 = system.createInstructor("i1", "I1", "i1@x");
        Instructor i2 = system.createInstructor("i2", "I2", "i2@x");
        Workshop w = system.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@x");
        system.createTeam("T", w, List.of("s1"));

        system.associateWorkshopWithInstructor(i1, w);
        system.associateWorkshopWithInstructor(i2, w);

        same(i2, w.getInstructor(), "workshop instructor");
        equal(List.of(), i1.getWorkshops(), "old instructor lost the workshop");
        equal(List.of(w), i2.getWorkshops(), "new instructor has the workshop");
        equal(List.of(), system.getStudentsOfInstructor("i1"), "old instructor has no students");
        equal(List.of("s1"), ids(system.getStudentsOfInstructor("i2")), "new instructor has s1");
        same(i2, system.getStudent("s1").getInstructor(), "student instructor follows");
    }

    private static void associateTwice() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Instructor i1 = system.createInstructor("i1", "I1", "i1@x");
        Workshop w = system.createWorkshop("A", "a");
        system.associateWorkshopWithInstructor(i1, w);
        system.associateWorkshopWithInstructor(i1, w);
        equal(1, i1.getWorkshops().size(), "workshop listed once");
    }

    private static void foreignObjectsRejected() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        WorkshopSystemImpl other = new WorkshopSystemImpl();
        Instructor mine = system.createInstructor("i1", "I1", "i1@x");
        Workshop myWorkshop = system.createWorkshop("A", "a");
        Instructor foreign = other.createInstructor("i1", "I1", "i1@x");
        Workshop foreignWorkshop = other.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@x");

        throwsIllegalArgument(() -> system.associateWorkshopWithInstructor(foreign, myWorkshop), "foreign instructor");
        throwsIllegalArgument(() -> system.associateWorkshopWithInstructor(mine, foreignWorkshop), "foreign workshop");
        throwsIllegalArgument(() -> system.createTeam("T", foreignWorkshop, List.of("s1")), "team in foreign workshop");
        throwsIllegalArgument(() -> system.associateWorkshopWithInstructor(null, myWorkshop), "null instructor");
        equal(null, myWorkshop.getInstructor(), "workshop still has no instructor");
        equal(List.of("s1"), ids(system.getFreeStudents()), "s1 still free");
    }

    private static void returnedListsAreCopies() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Instructor i1 = system.createInstructor("i1", "I1", "i1@x");
        Workshop w = system.createWorkshop("A", "a");
        system.associateWorkshopWithInstructor(i1, w);
        system.createStudent("s1", "S1", "s1@x");
        system.createStudent("s2", "S2", "s2@x");
        Team t = system.createTeam("T", w, List.of("s1", "s2"));

        system.getStudents().clear();
        system.getInstructors().clear();
        system.getWorkshops().clear();
        system.getTeams().clear();
        i1.getWorkshops().clear();
        t.getStudents().clear();
        system.getStudent("s1").getTeamMates().clear();

        equal(2, system.getStudents().size(), "students");
        equal(1, system.getInstructors().size(), "instructors");
        equal(1, system.getWorkshops().size(), "workshops");
        equal(1, system.getTeams().size(), "teams");
        equal(1, i1.getWorkshops().size(), "instructor workshops");
        equal(2, t.getStudents().size(), "team students");
        equal(List.of("s2"), ids(system.getStudent("s1").getTeamMates()), "team mates");
    }

    private static void unknownKeys() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        equal(null, system.getStudent("nope"), "student");
        equal(null, system.getInstructor("nope"), "instructor");
        equal(null, system.getWorkshop(42), "workshop");
        equal(null, system.getTeam(42), "team");
        equal(null, system.getStudent(null), "null student id");
        equal(List.of(), system.getStudentsOfInstructor("nope"), "students of instructor");
        equal(List.of(), system.getStudentsOfWorkshop(42), "students of workshop");
    }

    private static void instructorDerivedLive() {
        WorkshopSystemImpl system = new WorkshopSystemImpl();
        Workshop w = system.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@x");
        system.createTeam("T", w, List.of("s1"));
        equal(null, system.getStudent("s1").getInstructor(), "no instructor yet");
        Instructor i1 = system.createInstructor("i1", "I1", "i1@x");
        system.associateWorkshopWithInstructor(i1, w);
        same(i1, system.getStudent("s1").getInstructor(), "instructor after association");
    }

    // ---------------------------------------------------------------- tiny test harness

    private static void test(String name, Runnable body) {
        try {
            body.run();
            passed++;
            System.out.println("  [PASS] " + name);
        } catch (AssertionError | RuntimeException e) {
            failures.add(name + " -> " + e.getMessage());
            System.out.println("  [FAIL] " + name + "\n         " + e.getMessage());
        }
    }

    private static void equal(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void same(Object expected, Object actual, String what) {
        if (expected != actual) {
            throw new AssertionError(what + ": expected the same object as <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void throwsIllegalArgument(Runnable action, String what) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(what + ": expected IllegalArgumentException but none was thrown");
    }

    private static List<String> ids(List<? extends Person> people) {
        List<String> ids = new ArrayList<>();
        for (Person p : people) {
            ids.add(p.getId());
        }
        return ids;
    }
}
