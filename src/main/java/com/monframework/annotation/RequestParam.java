package com.monframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {
    String value(); // Le nom du paramètre dans la requête
    boolean required() default true; // Si le paramètre est obligatoire
    String defaultValue() default ""; // Valeur par défaut si non fournie
}