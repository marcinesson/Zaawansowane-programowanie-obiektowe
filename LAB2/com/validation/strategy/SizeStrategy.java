package com.validation.strategy;
import java.lang.reflect.Field;
import java.util.Optional;
import com.validation.annotation.Size;

public class SizeStrategy implements com.validation.strategy.ValidationStrategy {
    @Override
    public Optional<String> validate(Field field, Object value) {
        if (field.isAnnotationPresent(Size.class) && value != null) {
            Size annotation = field.getAnnotation(Size.class);
            int length = value.toString().length();
            if (length < annotation.min() || length > annotation.max()) {
                String errorInfo = String.format("Pole %s: %s", field.getName(),
                        annotation.message()
                                .replace("{min}", String.valueOf(annotation.min()))
                                .replace("{max}", String.valueOf(annotation.max())));
                return Optional.of(errorInfo);
            }
        }
        return Optional.empty();
    }
}