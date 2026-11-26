# EX2 – Maven Evolution: work plan

The guide (`docs/EX2 - Maven Evolution.docx`) is the source of truth; this plan only orders the work.

## Workflow per stage

1. Present every architecture decision of the stage and wait for approval.
2. Implement. The build must pass, and from stage 3 on the tester runs before every commit. A tester flow that already passed must never fail again. `RESULT: ALL OK` is required from stage 8 on. Until then the flows of features not built yet are expected to fail: 10 (e-mail) until stage 7, 11–14 (JSON) until stage 8. Flow 15 (CSV) reports `SKIPPED` until stage 10.
3. One commit per stage, pushed to `origin main` right away.

The submission tag `EX2-Maven-Evolution` is placed only when we decide to submit (see the end).

## Decisions

- Maven Wrapper (Maven is not installed on the development machine).
- Jackson for JSON, commons-validator for e-mails, commons-csv for the CSV import (guide §6.1 wants the CSV as a program argument, §8.1 in the app's resources: `Main` does both, argument first), Log4j 2 for logging, JUnit Jupiter 6 (`junit-jupiter`, declared directly in the root `dependencyManagement` with `test` scope, no BOM: guide §3.4 wants every library listed there).
- Uber jar with maven-assembly-plugin (jar-with-dependencies, `appendAssemblyId=false`, `Multi-Release` manifest entry for Log4j), named `workshop-cli.jar`.
- All bonuses: CSV import, tester in the `verify` phase (exec-maven-plugin), per-area managers in `logic-impl`.
- Root pom at the repository root. The given jars are in `lib/` and are installed with `install:install-file -DgeneratePom=true` (see README). `Ex1/` stays frozen.

## Stages

| # | Stage | Status |
|---|---|---|
| 0 | CLAUDE.md and project skills | done |
| 1 | Maven Wrapper, API jars in `lib/`, install commands in README, `target/` ignored | done |
| 2 | Multi-module skeleton (root, core, model-impl, logic-impl, app) | done |
| 3 | Port the EX1 code (packages `patmal.workshops.student.impl.*`, API 2.0, exports stubbed) | done (tester: flows 1–9 OK) |
| 4 | Own tests as JUnit tests | done (12 JUnit tests; tester: flows 1–9 OK) |
| 5 | Split `WorkshopSystem` into per-area managers | done (package-private managers; 12 tests and tester flows 1–9 OK) |
| 6 | Logging with Log4j 2 | done (Log4j 2.26.1; info on creation, warn on rejection; 12 tests and tester flows 1–9 OK) |
| 7 | E-mail validation (commons-validator) | done (commons-validator 1.11.0, transitive deps kept; 28 tests and tester flows 1–10 OK) |
| 8 | JSON exports (Jackson) | done (Jackson 3.2.4, package-private `JsonExporter` with output records; 36 tests; tester flows 1–14 OK, `RESULT: ALL OK`) |
| 9 | `app`: `Main` and the uber jar `workshop-cli.jar` | done (maven-assembly-plugin 3.8.0, demo data in code; jar has one `WorkshopSystem` implementation, `log4j2.xml`, no test libraries; tester `RESULT: ALL OK`, flow 15 `SKIPPED`) |
| 10 | Bonus: CSV import (`Main` reads students from the CSV given as argument, else from the `students.csv` packed in the jar) | done (commons-csv 1.14.1, brings commons-io and commons-codec; package-private `CsvImporter`; 49 tests; tester flows 1–15 OK, `RESULT: ALL OK`) |
| 11 | Bonus: tester in the `verify` phase (proven to fail on a deliberate break, never committed) | done (exec-maven-plugin 3.6.4, `exec:exec` with `${java.home}/bin/java` against the uber jar; tester jar located with `dependency:properties`; `-DskipTests` skips it; break "first workshop id 0" failed `verify` with `-Dmaven.test.failure.ignore=true`, reverted) |
| 12 | Cleanup: remove `artifacts/`, finish README | |
| 13 | **Pre-submission testing** (below) | |

## Stage 13: pre-submission testing

A dedicated phase on the final state of `main`, after all features and the cleanup. Nothing new is added here; every problem found is fixed in its own commit and the whole phase is run again.

1. **Grader simulation.** Fresh clone of `origin/main` into an empty folder, with an empty local Maven repository (`-Dmaven.repo.local=<empty folder>`). Run the README install commands exactly as written, then `mvnw clean install` from the root. Expected: build success and `app/target/workshop-cli.jar`.
2. **Both wrapper scripts.** The same build with `mvnw.cmd` (Windows) and `./mvnw` (POSIX shell), and once with JDK 25 as well as the default JDK.
3. **Tester.** `java -cp "lib/workshop-tester-2.0.jar;app/target/workshop-cli.jar" patmal.workshops.main.Main` reports `RESULT: ALL OK`, with flows 10–15 passing (15 not `SKIPPED`, since the CSV bonus is done).
4. **Application.** `java -jar workshop-cli.jar students.csv` runs, logs meaningful lines, and writes `instructors.json`, `students.json` and `teams.json` to the current folder. Check the files by hand against guide §5.1: fields, nesting, `null` relations, creation order, UTF-8.
5. **Uber jar contents.** The manifest has `Main-Class`. The jar holds exactly one `WorkshopSystem` implementation, plus the Log4j configuration. It holds no tester or JUnit classes.
6. **Build rules.** No `<version>` of a dependency or plugin in any child pom. Every version is a root property. `dependency:tree` and `dependency:analyze` show no surprises, and every transitive dependency can be explained.
7. **Bonus check.** A deliberate break (first workshop id 0) makes `mvnw clean install` fail. The own JUnit tests catch it first, so also run `mvnw clean install -Dmaven.test.failure.ignore=true`: it must fail in `app`'s `verify` phase because of the tester. The break is reverted and never committed.
8. **Own tests.** All JUnit tests pass in the build.
9. **Code review.** `java-code-review` skill on the whole implementation, within the API contract (`null` for unknown ids, `IllegalArgumentException` for invalid input, single-threaded).
10. **Guide checklist.** Walk through the guide section by section and note the evidence for every requirement.

## Submission

Only when we decide to submit: place the tag `EX2-Maven-Evolution` (exact name, case-sensitive) on the tested commit and push the tag. The grader checks out the tag and runs `mvn clean install` in the root, expecting the uber jar.
