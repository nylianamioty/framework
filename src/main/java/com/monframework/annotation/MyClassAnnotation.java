package com.monframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyClassAnnotation {
    String value() default "Classe non décrite";
    String author() default "Auteur inconnu";
    String version() default "1.0";
}