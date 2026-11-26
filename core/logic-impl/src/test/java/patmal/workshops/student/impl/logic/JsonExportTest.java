package patmal.workshops.student.impl.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;

/**
 * Own tests of the three JSON exports. Each file is read back with Jackson and compared with the
 * expected tree: object fields may come in any order, but every field, every value type (a number
 * is not text) and the order of every array must match.
 */
class JsonExportTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @TempDir
    Path folder;

    private WorkshopSystemImpl system;

    @BeforeEach
    void createSystem() {
        system = new WorkshopSystemImpl();
    }

    /**
     * i1 teaches w1 (team Alpha: s1, s2) and w2 (no teams); i2 teaches nothing.
     * w3 has no instructor (team Beta: s3); s4 stays free.
     */
    private void fillDemoData() {
        Instructor i1 = system.createInstructor("i1", "Dana Levi", "dana@univ.edu");
        system.createInstructor("i2", "Noa Bar", "noa@univ.edu");
        Workshop w1 = system.createWorkshop("Java", "Java basics");
        Workshop w2 = system.createWorkshop("Maven", "Builds");
        Workshop w3 = system.createWorkshop("Git", "Versions");
        system.associateWorkshopWithInstructor(i1, w1);
        system.associateWorkshopWithInstructor(i1, w2);
        system.createStudent("s1", "Alice", "alice@students.edu");
        system.createStudent("s2", "Bob", "bob@students.edu");
        system.createStudent("s3", "Carol", "carol@students.edu");
        system.createStudent("s4", "David", "david@students.edu");
        system.createTeam("Alpha", w1, List.of("s1", "s2"));
        system.createTeam("Beta", w3, List.of("s3"));
    }

    @Test
    @DisplayName("instructors export: instructor -> workshops -> teams -> student details")
    void instructorsExport() {
        fillDemoData();
        File file = folder.resolve("instructors.json").toFile();

        system.exportInstructorsDataToJson(file);

        assertEquals(tree("""
                [ { "id": "i1", "name": "Dana Levi", "email": "dana@univ.edu",
                    "workshops": [
                      { "id": 1, "name": "Java", "description": "Java basics",
                        "teams": [ { "id": 1, "name": "Alpha", "students": [
                            { "id": "s1", "name": "Alice", "email": "alice@students.edu" },
                            { "id": "s2", "name": "Bob", "email": "bob@students.edu" } ] } ] },
                      { "id": 2, "name": "Maven", "description": "Builds", "teams": [] } ] },
                  { "id": "i2", "name": "Noa Bar", "email": "noa@univ.edu", "workshops": [] } ]
                """), read(file));
    }

    @Test
    @DisplayName("students export: team with student ids, workshop, instructor; free student has null team")
    void studentsExport() {
        fillDemoData();
        File file = folder.resolve("students.json").toFile();

        system.exportStudentsDataToJson(file);

        String alpha = """
                { "id": 1, "name": "Alpha", "studentIds": [ "s1", "s2" ],
                  "workshop": { "id": 1, "name": "Java", "description": "Java basics",
                    "instructor": { "id": "i1", "name": "Dana Levi", "email": "dana@univ.edu" } } }
                """;
        assertEquals(tree("""
                [ { "id": "s1", "name": "Alice", "email": "alice@students.edu", "team": %s },
                  { "id": "s2", "name": "Bob", "email": "bob@students.edu", "team": %s },
                  { "id": "s3", "name": "Carol", "email": "carol@students.edu",
                    "team": { "id": 2, "name": "Beta", "studentIds": [ "s3" ],
                      "workshop": { "id": 3, "name": "Git", "description": "Versions", "instructor": null } } },
                  { "id": "s4", "name": "David", "email": "david@students.edu", "team": null } ]
                """.formatted(alpha, alpha)), read(file));
    }

    @Test
    @DisplayName("teams export: team -> student details, workshop -> instructor (or null)")
    void teamsExport() {
        fillDemoData();
        File file = folder.resolve("teams.json").toFile();

        system.exportTeamsDataToJson(file);

        assertEquals(tree("""
                [ { "id": 1, "name": "Alpha",
                    "students": [ { "id": "s1", "name": "Alice", "email": "alice@students.edu" },
                                  { "id": "s2", "name": "Bob", "email": "bob@students.edu" } ],
                    "workshop": { "id": 1, "name": "Java", "description": "Java basics",
                      "instructor": { "id": "i1", "name": "Dana Levi", "email": "dana@univ.edu" } } },
                  { "id": 2, "name": "Beta",
                    "students": [ { "id": "s3", "name": "Carol", "email": "carol@students.edu" } ],
                    "workshop": { "id": 3, "name": "Git", "description": "Versions", "instructor": null } } ]
                """), read(file));
    }

    @Test
    @DisplayName("an empty system exports three empty arrays")
    void emptySystem() {
        File instructors = folder.resolve("i.json").toFile();
        File students = folder.resolve("s.json").toFile();
        File teams = folder.resolve("t.json").toFile();

        system.exportInstructorsDataToJson(instructors);
        system.exportStudentsDataToJson(students);
        system.exportTeamsDataToJson(teams);

        assertEquals(tree("[]"), read(instructors), "instructors");
        assertEquals(tree("[]"), read(students), "students");
        assertEquals(tree("[]"), read(teams), "teams");
    }

    @Test
    @DisplayName("an existing, longer file is completely overwritten")
    void overwritesExistingFile() throws IOException {
        Path path = folder.resolve("teams.json");
        Files.writeString(path, "x".repeat(10_000), StandardCharsets.UTF_8);

        system.exportTeamsDataToJson(path.toFile());

        assertEquals(tree("[]"), read(path.toFile()));
    }

    @Test
    @DisplayName("a file in a missing folder is an UncheckedIOException, a null file an IllegalArgumentException")
    void invalidTargets() {
        File missing = folder.resolve("no-such-folder").resolve("out.json").toFile();

        assertThrows(UncheckedIOException.class, () -> system.exportInstructorsDataToJson(missing), "instructors");
        assertThrows(UncheckedIOException.class, () -> system.exportStudentsDataToJson(missing), "students");
        assertThrows(UncheckedIOException.class, () -> system.exportTeamsDataToJson(missing), "teams");
        assertThrows(IllegalArgumentException.class, () -> system.exportInstructorsDataToJson(null), "null");
    }

    @Test
    @DisplayName("non-ASCII text is written in UTF-8")
    void utf8() throws IOException {
        String hebrew = "שרה כהן";   // "Sara Cohen" in Hebrew
        system.createStudent("s1", hebrew, "sara@students.edu");
        system.createStudent("s2", "Renée Müller", "renee@students.edu");
        Path path = folder.resolve("students.json");

        system.exportStudentsDataToJson(path.toFile());

        String text = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(text.contains(hebrew), "Hebrew name in UTF-8");
        assertTrue(text.contains("Renée Müller"), "accented name in UTF-8");
    }

    @Test
    @DisplayName("exports do not change the system, and later changes show up in the next export")
    void exportsDoNotChangeTheSystem() {
        fillDemoData();
        File file = folder.resolve("students.json").toFile();

        system.exportInstructorsDataToJson(folder.resolve("i.json").toFile());
        system.exportStudentsDataToJson(file);
        system.exportTeamsDataToJson(folder.resolve("t.json").toFile());

        assertEquals(4, system.getStudents().size(), "students");
        assertEquals("s4", system.getFreeStudents().get(0).getId(), "free student");
        assertEquals(4, system.createWorkshop("New", "n").getId(), "next workshop id");

        system.createTeam("Gamma", system.getWorkshop(4), List.of("s4"));
        system.exportStudentsDataToJson(file);
        assertEquals("Gamma", read(file).get(3).get("team").get("name").asString(), "s4 after the change");
    }

    private static JsonNode tree(String json) {
        return MAPPER.readTree(json);
    }

    private static JsonNode read(File file) {
        return MAPPER.readTree(file);
    }
}
