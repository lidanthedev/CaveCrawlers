package me.lidan.cavecrawlers.requirements;

import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.entity.Player;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class RequirementEvaluator {
    private static final Pattern UNSAFE_MVEL_TOKENS = Pattern.compile(
            "(?i)(?:\\bnew\\b|\\bclass\\b|\\bgetClass\\b|\\bimport\\b|\\bRuntime\\b|\\bSystem\\b|\\bProcessBuilder\\b|\\bClassLoader\\b|\\bforName\\b|\\bloadClass\\b|\\bexec\\b|\\bexit\\b|\\bsetAccessible\\b|\\binvoke\\b|\\bconstructor\\b|\\breflect\\b|\\bThread\\b|\\bwait\\b|\\bnotifyAll?\\b)|[;\\n\\r{}$]"
    );
    private static final Set<String> ALLOWED_ROOT_IDENTIFIERS = Set.of(
            "player",
            "StatType",
            "true",
            "false",
            "null",
            "this",
            "if",
            "for",
            "do",
            "break",
            "continue",
            "try",
            "catch",
            "finally",
            "throw",
            "var",
            "def",
            "in",
            "not",
            "and",
            "or",
            "contains",
            "instanceof",
            "empty",
            "return",
            "assert",
            "function",
            "import",
            "import_static",
            "isdef",
            "proto",
            "switch",
            "with",
            "while",
            "until",
            "foreach",
            "else",
            "cond",
            "convertable_to",
            "union",
            "similarity",
            "soundex",
            "soundslike",
            "xswap",
            "xswap_op"
    );

    public static Object eval(Player player, String expression) {
        try {
            validateMvelExpression(expression);

            ParserContext parserContext = new ParserContext();
            parserContext.setStrongTyping(true);
            parserContext.setStrictTypeEnforcement(true);
            parserContext.getParserConfiguration().setAllowNakedMethCall(false);
            parserContext.getParserConfiguration().setAllowBootstrapBypass(false);
            parserContext.setInputs(Map.of("player", SafePlayerContext.class, "StatType", StatType.class));

            Serializable compiled = MVEL.compileExpression(expression, parserContext);
            Map<String, Object> vars = Map.of("player", new SafePlayerContext(player), "StatType", StatType.class);
            return MVEL.executeExpression(compiled, vars);
        } catch (Exception e) {
            log.warn("Failed to evaluate MVEL expression [{}] for {}", expression, player.getName(), e);
            throw new RuntimeException("Failed to evaluate MVEL expression", e);
        }
    }

    private static void validateMvelExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }
        if (expression.length() > 511) {
            throw new IllegalArgumentException("Expression is too long");
        }
        if (UNSAFE_MVEL_TOKENS.matcher(expression).find()) {
            throw new IllegalArgumentException("Expression contains a blocked token");
        }
        validateRootIdentifiers(expression);
    }

    private static void validateRootIdentifiers(String expression) {
        int length = expression.length();
        int index = 0;
        while (index < length) {
            char current = expression.charAt(index);

            if (current == '\'' || current == '"') {
                index = skipQuotedLiteral(expression, index, current);
                continue;
            }
            if (current == '/' && index + 1 < length) {
                char next = expression.charAt(index + 1);
                if (next == '/') {
                    index = skipLineComment(expression, index + 2);
                    continue;
                }
                if (next == '*') {
                    index = skipBlockComment(expression, index + 2);
                    continue;
                }
            }
            if (isNumberStart(expression, index)) {
                index = skipNumber(expression, index);
                continue;
            }
            if (isIdentifierStart(current)) {
                int start = index;
                index++;
                while (index < length && isIdentifierPart(expression.charAt(index))) {
                    index++;
                }
                if (isRootIdentifier(expression, start) && !ALLOWED_ROOT_IDENTIFIERS.contains(expression.substring(start, index))) {
                    throw new IllegalArgumentException("Expression references a non-whitelisted root identifier: " + expression.substring(start, index));
                }
                continue;
            }

            index++;
        }
    }

    private static int skipQuotedLiteral(String expression, int start, char quote) {
        int index = start + 1;
        while (index < expression.length()) {
            char current = expression.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            if (current == quote) {
                return index + 1;
            }
            index++;
        }
        return expression.length();
    }

    private static int skipLineComment(String expression, int start) {
        int index = start;
        while (index < expression.length()) {
            char current = expression.charAt(index);
            if (current == '\n' || current == '\r') {
                return index;
            }
            index++;
        }
        return expression.length();
    }

    private static int skipBlockComment(String expression, int start) {
        int index = start;
        while (index + 1 < expression.length()) {
            if (expression.charAt(index) == '*' && expression.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return expression.length();
    }

    private static int skipNumber(String expression, int start) {
        int length = expression.length();
        int index = start;

        if (expression.charAt(index) == '.') {
            index++;
        }

        if (index + 1 < length && expression.charAt(index) == '0' && (expression.charAt(index + 1) == 'x' || expression.charAt(index + 1) == 'X')) {
            index += 2;
            while (index < length && isHexDigitOrUnderscore(expression.charAt(index))) {
                index++;
            }
            return index;
        }
        if (index + 1 < length && expression.charAt(index) == '0' && (expression.charAt(index + 1) == 'b' || expression.charAt(index + 1) == 'B')) {
            index += 2;
            while (index < length && (expression.charAt(index) == '0' || expression.charAt(index) == '1' || expression.charAt(index) == '_')) {
                index++;
            }
            return index;
        }

        while (index < length && isDigitOrUnderscore(expression.charAt(index))) {
            index++;
        }

        if (index < length && expression.charAt(index) == '.') {
            index++;
            while (index < length && isDigitOrUnderscore(expression.charAt(index))) {
                index++;
            }
        }

        if (index < length && (expression.charAt(index) == 'e' || expression.charAt(index) == 'E')) {
            int exponentIndex = index + 1;
            if (exponentIndex < length && (expression.charAt(exponentIndex) == '+' || expression.charAt(exponentIndex) == '-')) {
                exponentIndex++;
            }
            boolean hasExponentDigits = false;
            while (exponentIndex < length && isDigitOrUnderscore(expression.charAt(exponentIndex))) {
                hasExponentDigits = true;
                exponentIndex++;
            }
            if (hasExponentDigits) {
                index = exponentIndex;
            }
        }

        if (index < length && isNumericSuffix(expression.charAt(index))) {
            index++;
        }

        return index;
    }

    private static boolean isRootIdentifier(String expression, int start) {
        int index = start - 1;
        while (index >= 0 && Character.isWhitespace(expression.charAt(index))) {
            index--;
        }
        return index < 0 || expression.charAt(index) != '.';
    }

    private static boolean isNumberStart(String expression, int index) {
        char current = expression.charAt(index);
        if (Character.isDigit(current)) {
            return true;
        }
        return current == '.' && index + 1 < expression.length() && Character.isDigit(expression.charAt(index + 1));
    }

    private static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_' || value == '$';
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private static boolean isDigitOrUnderscore(char value) {
        return Character.isDigit(value) || value == '_';
    }

    private static boolean isHexDigitOrUnderscore(char value) {
        return Character.isDigit(value) || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F') || value == '_';
    }

    private static boolean isNumericSuffix(char value) {
        return value == 'd' || value == 'D' || value == 'f' || value == 'F' || value == 'l' || value == 'L';
    }
}
