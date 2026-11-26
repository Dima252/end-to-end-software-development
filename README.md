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

This builds every module, runs the own JUnit tests and produces the uber jar `app/target/workshop-cli.jar` (the application with all its runtime dependencies). In the `verify` phase the build then runs the course tester against that jar (exec-maven-plugin, in a separate JVM), and a tester failure fails the build. `-DskipTests` skips both the own tests and the tester.

Run the application with:

```
java -jar app/target/workshop-cli.jar [students.csv]
```

It fills a workshop system with demo data and writes `instructors.json`, `students.json` and `teams.json` to the current folder. The students are imported from the CSV file given as the argument; without an argument, the `students.csv` packed in the jar (`app/src/main/resources/students.csv`) is used. The file is UTF-8 with the header `id,name,email`; values may be quoted, e.g. `s2,"Friedman, Bob",bob@students.edu`. It must list at least 4 students: the first two form team Alpha, the third team Beta, and the rest stay free.

Run the course tester by hand against the uber jar (on Linux / macOS use `:` instead of `;`):

```
java -cp "lib/workshop-tester-2.0.jar;app/target/workshop-cli.jar" patmal.workshops.main.Main
```

The log level of the own code is `info` by default and can be changed with `-Dworkshop.log.level=...` (for example `warn` or `error`); the build's tester run uses `error`, so that only the tester's report is shown.

Show the dependencies of every module:

```
.\mvnw.cmd dependency:tree
```

### Project structure

```
pom.xml                    root: aggregator of core and app, parent of every module;
                           all versions (properties), dependencyManagement, pluginManagement
core/pom.xml               aggregator and parent of model-impl and logic-impl
  model-impl/              patmal.workshops.student.impl.model
                             PersonImpl, InstructorImpl, StudentImpl, WorkshopImpl, TeamImpl
  logic-impl/              patmal.workshops.student.impl.logic
                             WorkshopSystemImpl (the only WorkshopSystem implementation)
                             InstructorManager, WorkshopManager, StudentManager, TeamManager
                             JsonExporter, CsvImporter, Validation
                           src/test: JUnit tests, log4j2-test.xml
app/                       patmal.workshops.student.impl.app.Main
                           src/main/resources: log4j2.xml, students.csv
                           builds workshop-cli.jar and runs the tester in verify
lib/                       given API jars and tester (installed into ~/.m2, see above)
.mvn/wrapper/, mvnw, mvnw.cmd   Maven Wrapper
Ex1/                       EX1, kept unchanged (see below)
docs/                      exercise descriptions and the EX2 work plan
```

Every module has the groupId `patmal.workshops.student.impl` and inherits the version `1.0-SNAPSHOT` from the root. The modules refer to each other with `${project.version}`, managed in the root.

### Libraries

All versions are properties in the root pom, and every library is declared in the root `dependencyManagement`. The child poms list dependencies without versions.

| Library | Version | Used in (scope) | Why | Brings with it |
|---|---|---|---|---|
| Log4j 2 (`log4j-api`, `log4j-core`) | 2.26.1 | `log4j-api`: logic-impl, app (compile). `log4j-core`: app (runtime), logic-impl (test) | Self-contained logging, no extra facade. The code uses only the API; the backend is chosen by the application. | nothing |
| Apache Commons Validator | 1.11.0 | logic-impl (compile) | E-mail validation (`EmailValidator`), widely used and maintained by Apache | commons-beanutils 1.11.0, commons-digester 2.1 (read the validator's XML rule files), commons-logging 1.4.0 (its logging), commons-collections 3.2.2 (used by beanutils) |
| Jackson (`tools.jackson.core:jackson-databind`) | 3.2.4 | logic-impl (compile) | The standard Java JSON library. Version 3 is the line Spring Boot 4 uses, which the next exercises need. | jackson-core 3.2.4 (streaming parser and writer), jackson-annotations 2.22 (the annotations are shared by Jackson 2 and 3, so they stay on the 2.x line) |
| Apache Commons CSV | 1.14.1 | logic-impl (compile) | CSV import (bonus): quoted values, commas inside values, header check | commons-io 2.20.0, commons-codec 1.19.0 (internal helpers of commons-csv) |
| JUnit Jupiter (`junit-jupiter`) | 6.1.3 | logic-impl (test) | Own unit tests | the JUnit API, params and engine, junit-platform, opentest4j, apiguardian-api, jspecify (all test scope) |
| Course tester (`workshop-tester`) | 2.0 | app (test) | Runs in `verify`. Test scope keeps it out of the uber jar. | nothing (generated pom) |

Build plugins (versions in the root `pluginManagement`): maven-assembly-plugin 3.8.0 (uber jar, `jar-with-dependencies`), exec-maven-plugin 3.6.4 (tester in `verify`), maven-dependency-plugin 3.11.0 (finds the tester jar for the class path; Maven's default 3.7.0 cannot read Java 25 class files, so `dependency:analyze` failed), plus the default lifecycle plugins (clean, resources, compiler, surefire, jar, install) pinned to current releases.

### Design notes

- `WorkshopSystemImpl` is a thin class that delegates to four package-private managers (instructors, workshops, students, teams). JSON export and CSV import are package-private helpers in the same package.
- Every creation is logged at `info`, and every rejected operation at `warn` before the exception is rethrown.
- E-mails are checked with Commons Validator. An invalid or `null` e-mail throws `IllegalArgumentException`.
- JSON exports (Jackson): arrays in creation order, a missing relation is written as `null`, the nested objects never point back to an ancestor, files are UTF-8 and overwritten, and a missing folder throws `UncheckedIOException`.
- CSV import (Commons CSV): UTF-8 (a BOM is skipped), the header must be exactly `id,name,email`, and values are not trimmed. The import stops at the first rejected line with `IllegalArgumentException` ("Line N: ..."), keeping the students added before it. A file that cannot be read throws `UncheckedIOException`.
- The uber jar holds exactly one public `WorkshopSystem` implementation, so the tester finds it by scanning the class path.

## EX1 – Workshop Registration (Vanilla Java)

An in-memory system that manages instructors, workshops, students and teams. It implements the given `model` and `logic` API interfaces in plain Java 25, with no Maven and no frameworks. The submitted version is the tag `EX1-Vanilla`, including the submitted jars in `artifacts/`. That folder is no longer part of `main` since EX2 builds with Maven.

### Structure

```
Ex1/
  model/src/               patmal.workshops.model.impl  – Instructor, Student, Workshop, Team
  logic/src/               patmal.workshops.logic.impl  – WorkshopSystemImpl
  logic/test/              own tests (not packaged)
  build.cmd                build + test script
  *.jar                    given API jars and tester
```

`Ex1` opens in IntelliJ as a project with two modules, `model` and `logic`.

### Build and test

```
Ex1\build.cmd           compile, write jars to artifacts\, run own tests, run the tester
Ex1\build.cmd notest    compile and write jars only
```

`build.cmd` creates `artifacts\` in the repository root, which git ignores.

Run manually against the tester:

```
cd Ex1
java -cp "workshop-tester.jar;logic-1.0.jar;model-1.0.jar;..\artifacts\workshops-model-impl.jar;..\artifacts\workshops-logic-impl.jar" patmal.workshops.main.Main
```

Result at submission: **RESULT: ALL OK** (105/105 tester checks, 12/12 own tests).

### Design notes

- Data is kept in insertion-ordered maps keyed by id (`LinkedHashMap`), so every list comes back in creation order, as EX2 requires. Workshop and team ids are sequential from 1.
- A student stores only its team. Team name, instructor and team mates are derived from it, so they stay consistent.
- Both sides of every relation are updated together (workshop ↔ instructor, team ↔ student).
- `createTeam` validates all members before changing anything, so a rejected call changes no data and uses no id.
- Invalid operations throw `IllegalArgumentException`.
- Unknown ids: `getX(id)` returns `null` and `getStudentsOfX(id)` returns an empty list.
- All returned lists are copies, never `null`, and contain no duplicates.
- Model objects implement `equals`/`hashCode` by their own fields (not by reference). Relations that change or would form a cycle (a workshop's instructor, a team's students) are left out.
