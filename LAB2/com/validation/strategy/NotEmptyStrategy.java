package com.validation.strategy;
import java.lang.reflect.Field;
import java.util.Optional;
import com.validation.annotation.NotEmpty;

public class NotEmptyStrategy implements com.validation.strategy.ValidationStrategy {
    @Override
    public Optional<String> validate(Field field, Object value) {
        if (field.isAnnotationPresent(NotEmpty.class)) {
            NotEmpty annotation = field.getAnnotation(NotEmpty.class);
            if (value == null || value.toString().trim().isEmpty()) {
                return Optional.of(String.format("Pole %s: %s", field.getName(), annotation.message()));
            }
        }
        return Optional.empty();
    }
}