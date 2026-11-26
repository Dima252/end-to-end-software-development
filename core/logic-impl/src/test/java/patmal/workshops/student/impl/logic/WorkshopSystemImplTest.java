package patmal.workshops.student.impl.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Person;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;

/**
 * Own tests, ported from EX1. They complement the course tester and focus on cases it does not
 * cover. They use only the public WorkshopSystem API, so they do not depend on how the
 * implementation is organised inside.
 */
class WorkshopSystemImplTest {

    private WorkshopSystemImpl system;

    @BeforeEach
    void createSystem() {
        system = new WorkshopSystemImpl();
    }

    @Test
    @DisplayName("ids are sequential from 1 and kept per kind")
    void sequentialIds() {
        Workshop w1 = system.createWorkshop("A", "a");
        Workshop w2 = system.createWorkshop("B", "b");
        system.createStudent("s1", "S1", "s1@example.com");
        system.createStudent("s2", "S2", "s2@example.com");
        Team t1 = system.createTeam("T1", w2, List.of("s1"));
        Team t2 = system.createTeam("T2", w1, List.of("s2"));

        assertEquals(1, w1.getId(), "first workshop id");
        assertEquals(2, w2.getId(), "second workshop id");
        assertEquals(1, t1.getId(), "first team id");
        assertEquals(2, t2.getId(), "second team id");
    }

    @Test
    @DisplayName("free student has null / empty derived data")
    void freeStudent() {
        Student s = system.createStudent("s1", "S1", "s1@example.com");

        assertNull(s.getTeamName(), "team name");
        assertNull(s.getInstructor(), "instructor");
        assertEquals(List.of(), s.getTeamMates(), "team mates");
        assertEquals(List.of("s1"), ids(system.getFreeStudents()), "free students");
    }

    @Test
    @DisplayName("team mates exclude the student")
    void teamMatesExcludeSelf() {
        Workshop w = system.createWorkshop("A", "a");
        for (String id : List.of("s1", "s2", "s3")) {
            system.createStudent(id, id, id + "@example.com");
        }
        system.createTeam("T", w, List.of("s1", "s2", "s3"));

        assertEquals(List.of("s2", "s3"), ids(system.getStudent("s1").getTeamMates()), "s1 mates");
        assertEquals(List.of("s1", "s3"), ids(system.getStudent("s2").getTeamMates()), "s2 mates");
        assertEquals("T", system.getStudent("s3").getTeamName(), "s3 team name");
    }

    @Test
    @DisplayName("rejected createTeam is atomic and consumes no id")
    void rejectedTeamIsAtomic() {
        Workshop w = system.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@example.com");
        system.createStudent("s2", "S2", "s2@example.com");
        system.createStudent("s3", "S3", "s3@example.com");
        system.createTeam("Busy", w, List.of("s3"));

        assertThrows(IllegalArgumentException.class,
                () -> system.createTeam("X", w, List.of("s1", "s99")), "unknown student");
        assertThrows(IllegalArgumentException.class,
                () -> system.createTeam("X", w, List.of("s1", "s3")), "student already in a team");
        assertThrows(IllegalArgumentException.class,
                () -> system.createTeam("X", w, List.of("s1", "s2", "s1")), "same student twice");

        assertEquals(List.of("s1", "s2"), ids(system.getFreeStudents()), "s1, s2 still free");
        assertEquals(1, system.getTeams().size(), "still one team");
        assertEquals(List.of("s3"), ids(system.getTeam(1).getStudents()), "existing team untouched");
        assertEquals(2, system.createTeam("Ok", w, List.of("s1", "s2")).getId(), "next team still gets id 2");
    }

    @Test
    @DisplayName("duplicate ids are rejected per kind only")
    void duplicateIds() {
        system.createStudent("x1", "Student", "s@example.com");
        system.createInstructor("x1", "Instructor", "i@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> system.createStudent("x1", "Other", "o@example.com"), "duplicate student");
        assertThrows(IllegalArgumentException.class,
                () -> system.createInstructor("x1", "Other", "o@example.com"), "duplicate instructor");

        assertEquals("Student", system.getStudent("x1").getName(), "original student kept");
        assertEquals("Instructor", system.getInstructor("x1").getName(), "original instructor kept");
    }

    @ParameterizedTest(name = "student e-mail [{0}] is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "plainaddress", "user@", "@example.com", "a@b@example.com",
            "user name@example.com"})
    void invalidStudentEmailRejected(String email) {
        assertThrows(IllegalArgumentException.class,
                () -> system.createStudent("s1", "S1", email), "invalid e-mail");

        assertEquals(0, system.getStudents().size(), "no student created");
        assertEquals("s1@example.com", system.createStudent("s1", "S1", "s1@example.com").getEmail(),
                "same id still usable with a valid e-mail");
    }

    @ParameterizedTest(name = "instructor e-mail [{0}] is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "plainaddress", "user@", "@example.com", "a@b@example.com",
            "user name@example.com"})
    void invalidInstructorEmailRejected(String email) {
        assertThrows(IllegalArgumentException.class,
                () -> system.createInstructor("i1", "I1", email), "invalid e-mail");

        assertEquals(0, system.getInstructors().size(), "no instructor created");
        assertEquals("i1@example.com", system.createInstructor("i1", "I1", "i1@example.com").getEmail(),
                "same id still usable with a valid e-mail");
    }

    @Test
    @DisplayName("empty and null member lists are rejected")
    void emptyTeamRejected() {
        Workshop w = system.createWorkshop("A", "a");

        assertThrows(IllegalArgumentException.class,
                () -> system.createTeam("Empty", w, List.of()), "empty members");
        assertThrows(IllegalArgumentException.class,
                () -> system.createTeam("Null", w, null), "null members");

        assertEquals(0, system.getTeams().size(), "no team created");
    }

    @Test
    @DisplayName("re-associating a workshop updates both instructors")
    void reassociateWorkshop() {
        Instructor i1 = system.createInstructor("i1", "I1", "i1@example.com");
        Instructor i2 = system.createInstructor("i2", "I2", "i2@example.com");
        Workshop w = system.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@example.com");
        system.createTeam("T", w, List.of("s1"));

        system.associateWorkshopWithInstructor(i1, w);
        system.associateWorkshopWithInstructor(i2, w);

        assertSame(i2, w.getInstructor(), "workshop instructor");
        assertEquals(List.of(), i1.getWorkshops(), "old instructor lost the workshop");
        assertEquals(List.of(w), i2.getWorkshops(), "new instructor has the workshop");
        assertEquals(List.of(), system.getStudentsOfInstructor("i1"), "old instructor has no students");
        assertEquals(List.of("s1"), ids(system.getStudentsOfInstructor("i2")), "new instructor has s1");
        assertSame(i2, system.getStudent("s1").getInstructor(), "student instructor follows");
    }

    @Test
    @DisplayName("associating twice does not duplicate the workshop")
    void associateTwice() {
        Instructor i1 = system.createInstructor("i1", "I1", "i1@example.com");
        Workshop w = system.createWorkshop("A", "a");

        system.associateWorkshopWithInstructor(i1, w);
        system.associateWorkshopWithInstructor(i1, w);

        assertEquals(1, i1.getWorkshops().size(), "workshop listed once");
    }

    @Test
    @DisplayName("objects from another system are rejected")
    void foreignObjectsRejected() {
        WorkshopSystemImpl other = new WorkshopSystemImpl();
        Instructor mine = system.createInstructor("i1", "I1", "i1@example.com");
        Workshop myWorkshop = system.createWorkshop("A", "a");
        Instructor foreign = other.createInstructor("i1", "I1", "i1@example.com");
        Workshop foreignWorkshop = other.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> system.associateWorkshopWithInstructor(foreign, myWorkshop), "foreign instructor");
        assertThrows(IllegalArgumentException.class,
                () -> system.associateWorkshopWithInstructor(mine, foreignWorkshop), "foreign workshop");
        assertThrows(IllegalArgumentException.class,
                () -> system.createTeam("T", foreignWorkshop, List.of("s1")), "team in foreign workshop");
        assertThrows(IllegalArgumentException.class,
                () -> system.associateWorkshopWithInstructor(null, myWorkshop), "null instructor");

        assertNull(myWorkshop.getInstructor(), "workshop still has no instructor");
        assertEquals(List.of("s1"), ids(system.getFreeStudents()), "s1 still free");
    }

    @Test
    @DisplayName("returned lists are copies")
    void returnedListsAreCopies() {
        Instructor i1 = system.createInstructor("i1", "I1", "i1@example.com");
        Workshop w = system.createWorkshop("A", "a");
        system.associateWorkshopWithInstructor(i1, w);
        system.createStudent("s1", "S1", "s1@example.com");
        system.createStudent("s2", "S2", "s2@example.com");
        Team t = system.createTeam("T", w, List.of("s1", "s2"));

        system.getStudents().clear();
        system.getInstructors().clear();
        system.getWorkshops().clear();
        system.getTeams().clear();
        i1.getWorkshops().clear();
        t.getStudents().clear();
        system.getStudent("s1").getTeamMates().clear();

        assertEquals(2, system.getStudents().size(), "students");
        assertEquals(1, system.getInstructors().size(), "instructors");
        assertEquals(1, system.getWorkshops().size(), "workshops");
        assertEquals(1, system.getTeams().size(), "teams");
        assertEquals(1, i1.getWorkshops().size(), "instructor workshops");
        assertEquals(2, t.getStudents().size(), "team students");
        assertEquals(List.of("s2"), ids(system.getStudent("s1").getTeamMates()), "team mates");
    }

    @Test
    @DisplayName("unknown keys return null / empty list")
    void unknownKeys() {
        assertNull(system.getStudent("nope"), "student");
        assertNull(system.getInstructor("nope"), "instructor");
        assertNull(system.getWorkshop(42), "workshop");
        assertNull(system.getTeam(42), "team");
        assertNull(system.getStudent(null), "null student id");
        assertEquals(List.of(), system.getStudentsOfInstructor("nope"), "students of instructor");
        assertEquals(List.of(), system.getStudentsOfWorkshop(42), "students of workshop");
    }

    @Test
    @DisplayName("student instructor follows a later association")
    void instructorDerivedLive() {
        Workshop w = system.createWorkshop("A", "a");
        system.createStudent("s1", "S1", "s1@example.com");
        system.createTeam("T", w, List.of("s1"));
        assertNull(system.getStudent("s1").getInstructor(), "no instructor yet");

        Instructor i1 = system.createInstructor("i1", "I1", "i1@example.com");
        system.associateWorkshopWithInstructor(i1, w);

        assertSame(i1, system.getStudent("s1").getInstructor(), "instructor after association");
    }

    private static List<String> ids(List<? extends Person> people) {
        List<String> ids = new ArrayList<>();
        for (Person p : people) {
            ids.add(p.getId());
        }
        return ids;
    }
}
