package patmal.workshops.student.impl.logic;

/** Input checks shared by the managers. Every failed check is an {@link IllegalArgumentException}. */
final class Validation {

    private Validation() {
    }

    static void requireNonNull(Object value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }
    }
}
