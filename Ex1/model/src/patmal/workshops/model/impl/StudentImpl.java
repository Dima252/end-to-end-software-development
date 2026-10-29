package patmal.workshops.model.impl;

import java.util.ArrayList;
import java.util.List;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;

/**
 * A student only keeps a reference to its team. Team name, instructor and team mates are
 * all derived from that team, so they can never get out of sync.
 */
public class StudentImpl extends PersonImpl implements Student {

    private TeamImpl team;

    public StudentImpl(String id, String name, String email) {
        super(id, name, email);
    }

    /** The student's team, or null for a free student. */
    public TeamImpl getTeam() {
        return team;
    }

    public boolean isFree() {
        return team == null;
    }

    /** Only one side of the relation. Use {@link TeamImpl#addStudent} to keep both sides in sync. */
    void setTeam(TeamImpl team) {
        this.team = team;
    }

    @Override
    public String getTeamName() {
        return team == null ? null : team.getName();
    }

    @Override
    public Instructor getInstructor() {
        return team == null ? null : team.getWorkshop().getInstructor();
    }

    @Override
    public List<Student> getTeamMates() {
        List<Student> mates = new ArrayList<>();
        if (team != null) {
            for (StudentImpl member : team.members()) {
                if (member != this) {
                    mates.add(member);
                }
            }
        }
        return mates;
    }
}
