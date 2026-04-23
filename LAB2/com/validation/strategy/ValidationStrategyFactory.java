package com.validation.strategy;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import com.validation.annotation.*;

public class ValidationStrategyFactory {
    private static final Map<Class<? extends Annotation>, com.validation.strategy.ValidationStrategy> strategies = new HashMap<>();

    static {
        // Ręczne rejestrowanie strategii dla każdej adnotacji
        strategies.put(NotNull.class, new com.validation.strategy.NotNullStrategy());
        strategies.put(NotEmpty.class, new com.validation.strategy.NotEmptyStrategy());
        strategies.put(Size.class, new com.validation.strategy.SizeStrategy());
        strategies.put(NrIndeksu.class, new com.validation.strategy.NrIndeksuStrategy());
        strategies.put(Email.class, new com.validation.strategy.EmailStrategy());
    }

    private ValidationStrategyFactory() {
    }

    public static com.validation.strategy.ValidationStrategy getStrategy(Annotation annotation) {
        return strategies.get(annotation.annotationType());
    }
}