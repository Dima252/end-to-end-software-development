package patmal.workshops.student.impl.logic;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import patmal.workshops.api.model.Student;

/**
 * Registers students from a UTF-8 CSV file with the header {@code id,name,email}. Empty lines are
 * skipped and values may be quoted. Every line is passed to {@link StudentManager#create} exactly
 * as it is (no trimming), so the rules of createStudent apply.
 *
 * <p>Lines are processed in file order and the import stops at the first rejected line with an
 * {@link IllegalArgumentException}; the students of the lines before it stay registered. This is
 * what the guide requires, and the one place where an operation may change data and then fail.
 */
class CsvImporter {

    private static final Logger LOG = LogManager.getLogger(CsvImporter.class);

    private static final List<String> HEADER = List.of("id", "name", "email");

    /** Excel writes this byte order mark at the start of a "CSV UTF-8" file. */
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    private final StudentManager students;

    CsvImporter(StudentManager students) {
        this.students = students;
    }

    List<Student> importStudents(File source) {
        try {
            Validation.requireNonNull(source, "Source file");
            List<Student> imported = parseAndRegister(read(source));
            LOG.info("Imported {} students from {}", imported.size(), source.getAbsolutePath());
            return imported;
        } catch (IllegalArgumentException | UncheckedIOException e) {
            LOG.warn("Rejected import of students: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Reads the whole file first, so that an error while parsing the text can only be a format
     * error (IllegalArgumentException), never a read error (UncheckedIOException).
     */
    private static String read(File source) {
        try {
            String text = Files.readString(source.toPath(), StandardCharsets.UTF_8);
            return text.startsWith(BYTE_ORDER_MARK) ? text.substring(1) : text;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + source, e);
        }
    }

    private List<Student> parseAndRegister(String text) {
        List<Student> imported = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(text, CSVFormat.DEFAULT)) {
            Iterator<CSVRecord> records = parser.iterator();
            if (!records.hasNext()) {
                throw new IllegalArgumentException("The file has no header line, expected " + String.join(",", HEADER));
            }
            CSVRecord header = records.next();
            if (!header.toList().equals(HEADER)) {
                throw new IllegalArgumentException("Wrong header " + header.toList() + ", expected "
                        + String.join(",", HEADER));
            }
            while (records.hasNext()) {
                CSVRecord line = records.next();
                long lineNumber = parser.getCurrentLineNumber();
                if (line.size() != HEADER.size()) {
                    throw new IllegalArgumentException("Line " + lineNumber + ": expected 3 values, found "
                            + line.size());
                }
                try {
                    imported.add(students.create(line.get(0), line.get(1), line.get(2)));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException | UncheckedIOException e) {
            // The text is already in memory: the parser fails only on a malformed line,
            // for example a quote that is never closed.
            throw new IllegalArgumentException("Malformed CSV: " + e.getMessage(), e);
        }
        return imported;
    }
}
