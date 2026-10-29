# End to End Software Development

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

- Data is kept in insertion-ordered maps keyed by id. Workshop and team ids are sequential from 1.
- A student stores only its team. Team name, instructor and team mates are derived from it, so they stay consistent.
- Both sides of every relation are updated together (workshop ↔ instructor, team ↔ student).
- `createTeam` validates all members before changing anything, so a rejected call changes no data and uses no id.
- Invalid operations throw `IllegalArgumentException`.
- Unknown ids: `getX(id)` returns `null` and `getStudentsOfX(id)` returns an empty list.
- All returned lists are copies, never `null`, and contain no duplicates.
