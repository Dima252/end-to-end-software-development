package patmal.workshops.student.impl.logic;

import org.apache.commons.validator.routines.EmailValidator;

/** Input checks shared by the managers. Every failed check is an {@link IllegalArgumentException}. */
final class Validation {

    private Validation() {
    }

    static void requireNonNull(Object value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }
    }

    /** Accepts only a non-null address that commons-validator considers valid. */
    static void requireValidEmail(String email, String what) {
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException(what + " '" + email + "' is not valid");
        }
    }
}
