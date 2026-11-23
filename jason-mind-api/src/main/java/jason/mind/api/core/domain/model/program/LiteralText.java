package jason.mind.api.core.domain.model.program;

public final class LiteralText {

    private LiteralText() {
    }

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

    public static int arity(String literal) {
        String s = stripNegation(literal.trim());
        int open = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') return 0;
            if (c == '(') {
                open = i;
                break;
            }
        }
        if (open < 0) return 0;
        int depth = 0;
        int count = 1;
        boolean quoted = false;
        boolean empty = true;
        for (int i = open + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (c == '\\') i++;
                else if (c == '"') quoted = false;
                empty = false;
                continue;
            }
            if (c == '"') { quoted = true; empty = false; continue; }
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) break;
                depth--;
            } else if (c == ',' && depth == 0) count++;
            else if (!Character.isWhitespace(c)) empty = false;
        }
        return empty && count == 1 ? 0 : count;
    }

    private static String stripNegation(String s) {
        return s.startsWith("~") ? s.substring(1) : s;
    }
}
