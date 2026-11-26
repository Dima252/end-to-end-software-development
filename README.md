# End to End Software Development

## EX2 – Maven Evolution

The EX1 system as a Maven multi-module project, built from the repository root.

### Requirements

- JDK 25 or newer (`java` on the `PATH`, or `JAVA_HOME` set).
- No Maven installation needed: the Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.16 on its first run.

### One-time setup: install the given API jars

The course API jars and the tester are not on Maven Central. They are in `lib/` and have to be installed into the local Maven repository (`~/.m2`) once on every computer, from the repository root. The `-D` arguments are quoted so that the same lines also work in PowerShell.

Windows (cmd or PowerShell):

```
.\mvnw.cmd install:install-file "-Dfile=lib/model-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=model" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
.\mvnw.cmd install:install-file "-Dfile=lib/logic-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=logic" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
.\mvnw.cmd install:install-file "-Dfile=lib/workshop-tester-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=workshop-tester" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
```

Linux / macOS:

```
./mvnw install:install-file "-Dfile=lib/model-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=model" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
./mvnw install:install-file "-Dfile=lib/logic-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=logic" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
./mvnw install:install-file "-Dfile=lib/workshop-tester-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=workshop-tester" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
```

`-DgeneratePom=true` matters: each jar carries its own `pom.xml` inside `META-INF/maven/`, and that pom refers to a parent (`patmal.workshops:workshops-aggregator:1.0-SNAPSHOT`) that is not available. Without the flag, `install:install-file` installs that embedded pom and the build fails with "Failed to read artifact descriptor for patmal.workshops:model:jar:2.0". With the flag, a minimal pom (coordinates only) is generated instead.

The installed jars therefore have no dependencies, so Maven knows nothing about what they use: a module that uses `logic` must also declare `model`.

### Build and run

From the repository root (`./mvnw` on Linux / macOS):

```
.\mvnw.cmd clean install
```

This builds every module, runs the own JUnit tests and produces the uber jar `app/target/workshop-cli.jar` (the application with all its runtime dependencies). Run it with:

```
java -jar app/target/workshop-cli.jar [students.csv]
```

It fills a workshop system with demo data and writes `instructors.json`, `students.json` and `teams.json` to the current folder. The students are imported from the CSV file given as the argument; without an argument, the `students.csv` packed in the jar (`app/src/main/resources/students.csv`) is used. The file is UTF-8 with the header `id,name,email`; values may be quoted, e.g. `s2,"Friedman, Bob",bob@students.edu`. It must list at least 4 students: the first two form team Alpha, the third team Beta, and the rest stay free.

Run the course tester against the uber jar (on Linux / macOS use `:` instead of `;`):

```
java -cp "lib/workshop-tester-2.0.jar;app/target/workshop-cli.jar" patmal.workshops.main.Main
```

## EX1 – Workshop Registration (Vanilla Java)

An in-memory system that manages instructors, workshops, students and teams. It implements the given `model` and `logic` API interfaces in plain Java 25, with no Maven and no frameworks.

### Structure

```
artifacts/                 submitted jars (tested by the course tester)
  workshops-model-impl.jar
  workshops-logic-impl.jar
Ex1/
  model/src/               patmal.workshops.model.impl  – Instructor, Student, Workshop, Team
  logic/src/               patmal.workshops.logic.impl  – WorkshopSystemImpl
  logic/test/              own tests (not packaged)
  build.cmd                build + test script
  *.jar                    given API jars and tester
docs/                      exercise description
```

`Ex1` opens in IntelliJ as a project with two modules, `model` and `logic`.

### Build and test

```
Ex1\build.cmd           compile, write jars to artifacts\, run own tests, run the tester
Ex1\build.cmd notest    compile and write jars only
```

Run manually against the tester:

```
cd Ex1
java -cp "workshop-tester.jar;logic-1.0.jar;model-1.0.jar;..\artifacts\workshops-model-impl.jar;..\artifacts\workshops-logic-impl.jar" patmal.workshops.main.Main
```

Current result: **RESULT: ALL OK** (105/105 tester checks, 12/12 own tests).

### Design notes

- Data is kept in insertion-ordered maps keyed by id (`LinkedHashMap`), so every list comes back in creation order, as EX2 requires. Workshop and team ids are sequential from 1.
- A student stores only its team. Team name, instructor and team mates are derived from it, so they stay consistent.
- Both sides of every relation are updated together (workshop ↔ instructor, team ↔ student).
- `createTeam` validates all members before changing anything, so a rejected call changes no data and uses no id.
- Invalid operations throw `IllegalArgumentException`.
- Unknown ids: `getX(id)` returns `null` and `getStudentsOfX(id)` returns an empty list.
- All returned lists are copies, never `null`, and contain no duplicates.
- Model objects implement `equals`/`hashCode` by their own fields (not by reference). Relations that change or would form a cycle (a workshop's instructor, a team's students) are left out.
