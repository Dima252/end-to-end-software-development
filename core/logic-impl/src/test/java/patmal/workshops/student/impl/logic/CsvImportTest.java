package patmal.workshops.student.impl.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import patmal.workshops.api.model.Student;

/** Own tests of importStudentDataFromFile (the CSV bonus). */
class CsvImportTest {

    @TempDir
    Path folder;

    private WorkshopSystemImpl system;

    @BeforeEach
    void createSystem() {
        system = new WorkshopSystemImpl();
    }

    private File csv(String content) throws IOException {
        Path file = folder.resolve("students.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }

    private List<String> ids(List<Student> students) {
        List<String> ids = new ArrayList<>();
        for (Student student : students) {
            ids.add(student.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("valid file: quoted value with a comma and non-ASCII text, in file order")
    void validFile() throws IOException {
        List<Student> imported = system.importStudentDataFromFile(csv("""
                id,name,email
                s1,Alice Cohen,alice@students.edu
                s2,"Friedman, Bob",bob@students.edu
                s3,Zoë Katz,zoe@students.edu
                """));

        assertEquals(List.of("s1", "s2", "s3"), ids(imported));
        assertEquals(imported, system.getStudents());
        assertEquals("Friedman, Bob", system.getStudent("s2").getName());
        assertEquals("Zoë Katz", system.getStudent("s3").getName());
        assertEquals("zoe@students.edu", system.getStudent("s3").getEmail());
        assertEquals(3, system.getFreeStudents().size());
    }

    @Test
    @DisplayName("header only: nothing registered, empty list")
    void headerOnly() throws IOException {
        assertEquals(List.of(), system.importStudentDataFromFile(csv("id,name,email\n")));
        assertEquals(List.of(), system.getStudents());
    }

    @Test
    @DisplayName("empty lines are ignored, a missing final line break is fine")
    void emptyLinesIgnored() throws IOException {
        List<Student> imported = system.importStudentDataFromFile(csv(
                "id,name,email\n\ns1,Alice,alice@students.edu\n\n\ns2,Bob,bob@students.edu"));

        assertEquals(List.of("s1", "s2"), ids(imported));
    }

    @Test
    @DisplayName("Windows line breaks and a byte order mark are accepted")
    void windowsFile() throws IOException {
        List<Student> imported = system.importStudentDataFromFile(csv(
                "\uFEFFid,name,email\r\ns1,Alice,alice@students.edu\r\n"));

        assertEquals(List.of("s1"), ids(imported));
    }

    @Test
    @DisplayName("stops at the first rejected line; the lines before it stay registered")
    void stopsAtFirstRejectedLine() throws IOException {
        File file = csv("""
                id,name,email
                s1,Alice,alice@students.edu

                s2,Bob,not-an-email
                s3,Carol,carol@students.edu
                """);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> system.importStudentDataFromFile(file));

        assertTrue(e.getMessage().startsWith("Line 4: "), e.getMessage());
        assertEquals(List.of("s1"), ids(system.getStudents()));
        assertNull(system.getStudent("s3"));
    }

    @Test
    @DisplayName("a duplicate id is rejected like in createStudent")
    void duplicateId() throws IOException {
        system.createStudent("s1", "Alice", "alice@students.edu");
        File file = csv("id,name,email\ns2,Bob,bob@students.edu\ns1,Again,again@students.edu\n");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> system.importStudentDataFromFile(file));

        assertTrue(e.getMessage().startsWith("Line 3: "), e.getMessage());
        assertEquals("Alice", system.getStudent("s1").getName());
        assertEquals(List.of("s1", "s2"), ids(system.getStudents()));
    }

    @Test
    @DisplayName("a wrong header is rejected and nothing is registered")
    void wrongHeader() throws IOException {
        for (String header : List.of("name,id,email", "id,name", "id,name,email,team", "ID,Name,Email",
                "s1,Alice,alice@students.edu")) {
            File file = csv(header + "\ns1,Alice,alice@students.edu\n");
            assertThrows(IllegalArgumentException.class, () -> system.importStudentDataFromFile(file), header);
        }
        assertEquals(List.of(), system.getStudents());
    }

    @Test
    @DisplayName("an empty file has no header and is rejected")
    void emptyFile() throws IOException {
        File file = csv("");
        assertThrows(IllegalArgumentException.class, () -> system.importStudentDataFromFile(file));
    }

    @Test
    @DisplayName("a line with too few or too many values is rejected")
    void wrongNumberOfValues() throws IOException {
        File tooFew = csv("id,name,email\ns1,Alice\n");
        assertThrows(IllegalArgumentException.class, () -> system.importStudentDataFromFile(tooFew));

        File tooMany = csv("id,name,email\ns1,Alice,alice@students.edu,extra\n");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> system.importStudentDataFromFile(tooMany));
        assertTrue(e.getMessage().startsWith("Line 2: "), e.getMessage());

        assertEquals(List.of(), system.getStudents());
    }

    @Test
    @DisplayName("a quote that is never closed is a format error, not an I/O error")
    void unclosedQuote() throws IOException {
        File file = csv("id,name,email\ns1,Alice,alice@students.edu\ns2,\"Bob,bob@students.edu\n");

        assertThrows(IllegalArgumentException.class, () -> system.importStudentDataFromFile(file));
        assertEquals(List.of("s1"), ids(system.getStudents()));
    }

    @Test
    @DisplayName("values are not trimmed: a space before the e-mail makes it invalid")
    void valuesAreNotTrimmed() throws IOException {
        // also: the last line has no line break, and the line number is still right
        File file = csv("id,name,email\n\ns1,Alice, alice@students.edu");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> system.importStudentDataFromFile(file));
        assertTrue(e.getMessage().startsWith("Line 3: "), e.getMessage());
    }

    @Test
    @DisplayName("a missing file results in an UncheckedIOException")
    void missingFile() {
        File missing = folder.resolve("no-such-file.csv").toFile();
        assertThrows(UncheckedIOException.class, () -> system.importStudentDataFromFile(missing));
    }

    @Test
    @DisplayName("a null file is invalid input")
    void nullFile() {
        assertThrows(IllegalArgumentException.class, () -> system.importStudentDataFromFile(null));
    }
}
