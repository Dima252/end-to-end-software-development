package patmal.workshops.model.impl;

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

    @Override
    public String toString() {
        return id;
    }
}
