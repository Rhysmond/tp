package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's role in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidRole(String)}
 */
public class Role {

    public static final String MESSAGE_CONSTRAINTS = "Role should be one of the following: "
            + "Investor, Partner, Customer, or Lead (case-insensitive).\n"
            + "You can also use the numbers 1, 2, 3, 4 as shortcuts:\n"
            + "1 -> Investor, " + "2 -> Partner," + "3 -> Customer," + "4 -> Lead.";

    // Accepts only the 4 valid roles, case-insensitive
    private static final String VALIDATION_REGEX = "(?i)investor|partner|customer|lead";
    private static final String[] ROLE_SHORTCUTS = {"Investor", "Partner", "Customer", "Lead"};

    public final String value;

    /**
     * Constructs a {@code Role}.
     *
     * @param role A valid role string.
     */
    public Role(String role) {
        requireNonNull(role);
        String normalizedRole = normalizeRole(role);
        checkArgument(isValidRole(normalizedRole), MESSAGE_CONSTRAINTS);
        value = capitalize(normalizedRole.trim().toLowerCase());
    }

    /**
     * Converts numeric shortcuts to full role names.
     */
    public static String normalizeRole(String input) {
        input = input.trim();
        switch (input) {
        case "1": return "Investor";
        case "2": return "Partner";
        case "3": return "Customer";
        case "4": return "Lead";
        default: return input;
        }
    }

    /**
     * Returns true if a given string is a valid role.
     */
    public static boolean isValidRole(String test) {
        return test != null && test.trim().toLowerCase().matches(VALIDATION_REGEX);
    }

    /**
     * Capitalizes the first letter of the given string.
     */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Role)) {
            return false;
        }

        Role otherRole = (Role) other;
        return value.equalsIgnoreCase(otherRole.value);
    }

    @Override
    public int hashCode() {
        return value.toLowerCase().hashCode();
    }
}
