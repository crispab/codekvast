package se.crisp.codekvast.agent.config;

import lombok.EqualsAndHashCode;
import se.crisp.codekvast.agent.util.SignatureUtils;

import java.lang.reflect.Modifier;

/**
 * @author olle.hallin@crisp.se
 */
@EqualsAndHashCode
public class MethodVisibilityFilter {

    private static final int VISIBILITY_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    // The missing modifier
    private static final int PACKAGE_PRIVATE = 0x08;

    private final int mask;

    public MethodVisibilityFilter(String visibility) {
        mask = parseVisibility(visibility);
    }

    private int parseVisibility(String visibility) {
        boolean _public = false;
        boolean _protected = false;
        boolean _private = false;
        boolean _packagePrivate = false;
        boolean recognized = false;

        String value = visibility == null ? SignatureUtils.PUBLIC : visibility.trim().toLowerCase();
        if (value.equals(SignatureUtils.PUBLIC)) {
            recognized = true;
            _public = true;
        }
        if (value.equals(SignatureUtils.PROTECTED)) {
            recognized = true;
            _public = true;
            _protected = true;
        }

        if (value.equals(SignatureUtils.PACKAGE_PRIVATE) || value.equals("!private")) {
            recognized = true;
            _public = true;
            _protected = true;
            _packagePrivate = true;
        }

        if (value.equals(SignatureUtils.PRIVATE) || value.equals("all")) {
            recognized = true;
            _public = true;
            _protected = true;
            _packagePrivate = true;
            _private = true;
        }

        if (!recognized) {
            if (!value.isEmpty()) {
                System.err.println("Unrecognized value for methodVisibility: \"" + value + "\", assuming \"public\"");
            }
            _public = true;
        }

        int result = 0;
        if (_public) {
            result |= Modifier.PUBLIC;
        }
        if (_protected) {
            result |= Modifier.PROTECTED;
        }
        if (_packagePrivate) {
            result |= PACKAGE_PRIVATE;
        }
        if (_private) {
            result |= Modifier.PRIVATE;
        }
        return result;
    }

    public boolean selectsPublicMethods() {
        return (mask & Modifier.PUBLIC) != 0;
    }

    public boolean selectsProtectedMethods() {
        return (mask & Modifier.PROTECTED) != 0;
    }

    public boolean selectsPackagePrivateMethods() {
        return (mask & PACKAGE_PRIVATE) != 0;
    }

    public boolean selectsPrivateMethods() {
        return (mask & Modifier.PRIVATE) != 0;
    }

    /**
     * Given a Method.modifiers() tell whether a method should be included in the inventory or not.
     *
     * @param modifiers Returned from {@link java.lang.reflect.Method#getModifiers()}
     * @return True if any of the visibility bits in modifiers matches this object.
     */
    public boolean shouldInclude(int modifiers) {
        // Package private is an anomaly, since it is the lack of any visibility modifier.
        if (selectsPackagePrivateMethods() && (modifiers & VISIBILITY_MODIFIERS) == 0) {
            return true;
        }
        // At least one of the visibility bits match
        return (modifiers & mask) != 0;
    }

    @Override
    public String toString() {
        if (selectsPrivateMethods()) {
            return SignatureUtils.PRIVATE;
        }
        if (selectsPackagePrivateMethods()) {
            return SignatureUtils.PACKAGE_PRIVATE;
        }
        if (selectsProtectedMethods()) {
            return SignatureUtils.PROTECTED;
        }
        return SignatureUtils.PUBLIC;
    }
}
