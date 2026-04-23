package com.validation.annotation;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NrIndeksu {
    String message() default "Niepoprawny numer indeksu";
}