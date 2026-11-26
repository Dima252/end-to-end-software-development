package patmal.workshops.student.impl.model;

import java.util.Objects;

import patmal.workshops.api.model.Person;

/**
 * Common base for instructors and students. The id is supplied by the caller.
 */
public abstract class PersonImpl implements Person {

    private final String id;
    private final String name;
    private final String email;

    protected PersonImpl(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    /**
     * Two persons are equal when they are of the same kind (instructor or student) and have the
     * same id, name and e-mail. All these fields are final, so the hash code never changes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonImpl other = (PersonImpl) o;
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(email, other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email);
    }

    @Override
    public String toString() {
        return id;
    }
}
