package com.validation.strategy;
import java.lang.reflect.Field;
import java.util.Optional;
import com.validation.annotation.NotNull;

public class NotNullStrategy implements com.validation.strategy.ValidationStrategy {
    @Override
    public Optional<String> validate(Field field, Object value) {
        if (field.isAnnotationPresent(NotNull.class) && value == null) {
            NotNull annotation = field.getAnnotation(NotNull.class);
            return Optional.of(String.format("Pole %s: %s", field.getName(), annotation.message()));
        }
        return Optional.empty();
    }
}