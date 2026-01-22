package com.monframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SessionAttribute {
    String value(); // Nom de l'attribut de session
    String mode() default "read";
    
    boolean required() default false; // Si l'attribut est requis en lecture
    boolean createSession() default true; // Créer la session si elle n'existe pas
}