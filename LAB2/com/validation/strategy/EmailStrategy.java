package com.validation.strategy;
import java.lang.reflect.Field;
import java.util.Optional;
import com.validation.annotation.Email;

public class EmailStrategy implements com.validation.strategy.ValidationStrategy {
    @Override
    public Optional<String> validate(Field field, Object value) {
        if (field.isAnnotationPresent(Email.class) && value != null) {
            Email annotation = field.getAnnotation(Email.class);
            if (!value.toString().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return Optional.of(String.format("Pole %s: %s", field.getName(), annotation.message()));
            }
        }
        return Optional.empty();
    }
}