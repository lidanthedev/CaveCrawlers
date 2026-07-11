package me.lidan.cavecrawlers.requirements;

import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.stats.StatType;
import org.bukkit.entity.Player;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class RequirementEvaluator {
    private static final Pattern UNSAFE_MVEL_TOKENS = Pattern.compile(
            "(?i)(?:\\bnew\\b|\\bclass\\b|\\bgetClass\\b|\\bimport\\b|\\bRuntime\\b|\\bSystem\\b|\\bProcessBuilder\\b|\\bClassLoader\\b|\\bforName\\b|\\bloadClass\\b|\\bexec\\b|\\bexit\\b|\\bsetAccessible\\b|\\binvoke\\b|\\bconstructor\\b|\\breflect\\b|\\bThread\\b|\\bwait\\b|\\bnotifyAll?\\b)|[;\\n\\r{}$]"
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
    }
}
