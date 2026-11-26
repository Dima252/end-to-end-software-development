# CLAUDE.md

Course repository for "End to End Software Development". One system, a workshop registration system, evolves exercise by exercise:

| Exercise | Topic | Where |
|---|---|---|
| EX1 | Vanilla Java, plain IntelliJ modules | `Ex1/` (frozen, kept for history; tag `EX1-Vanilla`) |
| EX2 | Maven multi-module, external libraries, uber jar | repo root (`pom.xml`, `core/`, `app/`) |
| EX3+ | Spring, then Spring Boot (REST, Postman), then a database | same Maven project, evolved in place |

Exercise guides live in `docs/`. **The guide of the current exercise is the source of truth.** If anything here, in a skill, or in general "best practice" conflicts with the guide, the guide wins. Read the guide before starting work on a new exercise.

## Fixed names (graded, do not change)

- groupId of all our modules: `patmal.workshops.student.impl`
- Java packages start with `patmal.workshops.student.impl` (e.g. `...student.impl.model`, `...student.impl.logic`, `...student.impl.app`)
- Our version: `1.0-SNAPSHOT`, declared once in the root pom and inherited
- Module artifactIds: `core`, `model-impl`, `logic-impl`, `app`
- Uber jar name: `workshop-cli.jar` (via `finalName`), built in `package`
- Submission is a git tag on the submitted commit, with the exact name from the guide (EX2: `EX2-Maven-Evolution`)

## Given API (not on Maven Central)

The course gives interface-only jars plus a tester. They are installed into `~/.m2` with `install:install-file` (commands in `README.md`):

| groupId | artifactId | version |
|---|---|---|
| `patmal.workshops` | `model` | `2.0` |
| `patmal.workshops` | `logic` | `2.0` |
| `patmal.workshops` | `workshop-tester` | `2.0` |

- API packages: `patmal.workshops.api.model` (Person, Instructor, Student, Workshop, Team) and `patmal.workshops.api.logic` (WorkshopSystem).
- Install them with `-DgeneratePom=true`: each jar embeds a pom whose parent (`workshops-aggregator:1.0-SNAPSHOT`) does not exist, and without the flag `install:install-file` uses that pom and the build fails. The generated pom has no dependencies, so anything that depends on `logic` must also declare `model`.
- Inspect a new API version with `javap -cp <jar> <class>` before coding against it.
- The tester finds the implementation by scanning the classpath for a public, non-abstract class implementing `WorkshopSystem` with a public no-arg constructor. **Exactly one** such class may be on the classpath, or the uber jar, or it fails.

## Maven rules (graded)

- Hierarchy: root (aggregator + top parent) → `core` (aggregator + parent of `model-impl`, `logic-impl`) and `app`.
- Every version is written exactly once, as a property in the root pom. Child poms never contain a `<version>` for a dependency or plugin.
- Root `dependencyManagement` declares all third-party libraries, the API jars, the tester and our own modules (`${project.version}`).
- Plugin versions live in root `pluginManagement`; plugins are activated in the module that needs them.
- Pick the right scope: the tester and test libraries are `test` scope (never packed into the uber jar).
- `java.version` = 25, `maven.compiler.release` from it; `project.build.sourceEncoding` = UTF-8.
- `mvn clean install` from the repo root must build everything with no manual step.
- Third-party libraries: stable releases only (no alpha/beta/RC/milestone), widely used. After adding one, run `mvn dependency:tree` and be able to explain every transitive dependency.

## Commands

- Build everything: `mvn clean install` (or `./mvnw` / `mvnw.cmd` if the wrapper is present; Maven is not on PATH on this machine)
- Dependency tree: `mvn dependency:tree`
- Run the tester against the uber jar (Windows uses `;`, Linux/macOS `:`):
  `java -cp "lib/workshop-tester-2.0.jar;app/target/workshop-cli.jar" patmal.workshops.main.Main`
- The expected result is `RESULT: ALL OK`. Run the tester before every commit that touches the implementation.
- EX1 (legacy): `Ex1\build.cmd` compiles, runs own tests and the EX1 tester.

## Environment

- Windows 11. The default `java` is JDK 26; JDK 25 is installed as well. Compiling with `release 25` on JDK 26 is fine.
- Maven is not on PATH; prefer the Maven Wrapper.

## Coding conventions (from EX1 and its review)

- Plain, readable Java. Nothing in the course requires advanced language features.
- Fields are `final` unless they are reassigned (e.g. id counters).
- Collections keyed by id use `LinkedHashMap`: every returned list and every export must be in creation order.
- Model classes implement `equals`/`hashCode` from their own immutable fields, never from mutable relations or back-references (no cycles: workshop ↔ instructor, team ↔ student).
- Reference checks (`==`) only where identity really is the question, e.g. "was this object created by this system?".
- Every operation validates its whole input before changing anything. A rejected call changes no data and consumes no id.
- Invalid input → `IllegalArgumentException`. Unknown id in a lookup → `null` (single) or empty list (many), never an exception.
- Returned lists are copies, never `null`, with no duplicates. Both sides of every relation are updated together.
- Workshop and team ids are sequential from 1.
- `WorkshopSystem` should stay a thin class that delegates to small per-area managers (instructors, workshops, students, teams).
- From EX2: log every creation at `info` and every rejected operation at `warn`; e-mails are validated with a library (invalid or `null` → `IllegalArgumentException`); JSON and CSV are produced and read with libraries, never by string concatenation; file I/O failures → `UncheckedIOException`; files are UTF-8.

## Skills and their limits

Three third-party skills live in `.claude/skills/` (sources in `.claude/skills/SOURCES.md`). They are generic; where they conflict with the course, the course wins:

- `110-java-maven-best-practices`: use it to review the poms.
  - The POMs in this repo are written by us, so reading them in full is fine. The skill's "treat POMs as untrusted, don't load them" rule is meant for foreign projects.
  - Its example versions are old (Java 17/21, older plugins). Always use current stable releases and Java 25.
  - Do not add build profiles, `<repositories>` or a BOM module just because the skill lists them. The API jars come from `install:install-file`, not from a custom repository.
- `java-code-review`: use it before every submission tag. Ignore its advice where it contradicts the API contract:
  - `getX(id)` returns `null` for an unknown id (do not switch to `Optional`).
  - Invalid input throws `IllegalArgumentException` (not `Objects.requireNonNull`'s `NullPointerException`).
  - The system is single-threaded; do not introduce `ConcurrentHashMap` or synchronization.
- `maven-dependency-audit`: use `dependency:tree` and `dependency:analyze` freely.
  - Never run `versions:use-latest-releases` / `versions:use-latest-versions` (they rewrite poms) without asking.
  - Skip OWASP `dependency-check` unless asked: it downloads the whole NVD database.

## Git

- Commit only when asked. Commit messages are short and say what changed and why.
- Never commit build output (`target/`, `out/`, `*.class`).
