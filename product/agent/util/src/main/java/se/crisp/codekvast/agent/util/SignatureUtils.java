package se.crisp.codekvast.agent.util;

/**
 * Utility class for dealing with signatures.
 *
 * @author Olle Hallin
 */
public class SignatureUtils {
    private SignatureUtils() {
        // utility class
    }

    /**
     * Minimizes a signature: <ul> <li>Removes all modifiers (public, static, etc)</li> <li>Removes the return type.</li> </ul>
     * <p/>
     * Two overloaded Java signatures cannot differ only in modifiers or return type.
     *
     * @param signature The full method signature including modifiers and return type.
     * @return Only the method name and the parameter list
     */
    public static String minimizeSignature(String signature) {
        if (signature == null) {
            return null;
        }
        int pos = signature.indexOf("(");
        if (pos < 0) {
            return signature;
        }
        while (signature.charAt(pos) != ' ' && pos >= 0) {
            pos -= 1;
        }
        return signature.substring(pos + 1);
    }
}
