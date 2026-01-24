package com.monframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RoleAllowed {
    String[] value();
    
    // Attribut de session qui contient le rôle
    String roleAttribute() default "role";
    
    // URL de redirection si rôle non autorisé
    String redirect() default "/access-denied";
}