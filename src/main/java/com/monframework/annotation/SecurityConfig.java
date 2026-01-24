package com.monframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SecurityConfig {
    String authAttribute() default "authenticated"; // Attribut boolean pour auth
    String userAttribute() default "user"; // Attribut objet user
    String roleAttribute() default "role"; // Attribut rôle
    String loginUrl() default "/login"; // URL de login
    String accessDeniedUrl() default "/access-denied"; // URL accès refusé
}