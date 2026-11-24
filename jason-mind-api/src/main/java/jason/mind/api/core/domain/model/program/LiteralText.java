package jason.mind.api.core.domain.model.program;

/**
 * Utility that reads the functor and arity from the text of a literal.
 * Strong negation is ignored.
 *
 * @author Gabriel Lima
 */
public final class LiteralText {

    /**
     * Prevents instantiation.
     */
    private LiteralText() {
    }

    /**
     * Extracts the functor of a literal.
     *
     * @param literal text of the literal
     * @return text before the first parenthesis or bracket, without negation
     */
    public static String functor(String literal) {
        String s = stripNegation(literal.trim());
        int end = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[') {
                end = i;
                break;
            }
        }
        return s.substring(0, end);
    }

    /**
     * Counts the top-level arguments of a literal, skipping quoted strings and nested terms.
     *
     * @param literal text of the literal
     * @return number of arguments, or zero when the literal has no argument list
     */
    public static int arity(String literal) {
        String s = stripNegation(literal.trim());
        int open = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') {
                return 0;
            }
            if (c == '(') {
                open = i;
                break;
            }
        }
        if (open < 0) {
            return 0;
        }
        int depth = 0;
        int count = 1;
        boolean quoted = false;
        boolean empty = true;
        for (int i = open + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quoted = false;
                }
                empty = false;
                continue;
            }
            if (c == '"') {
                quoted = true;
                empty = false;
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                count++;
            } else if (!Character.isWhitespace(c)) {
                empty = false;
            }
        }
        return empty && count == 1 ? 0 : count;
    }

    /**
     * Removes the strong negation prefix.
     *
     * @param s text of the literal
     * @return text without a leading {@code ~}
     */
    private static String stripNegation(String s) {
        return s.startsWith("~") ? s.substring(1) : s;
    }
}
