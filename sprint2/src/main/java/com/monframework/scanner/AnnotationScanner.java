package com.monframework.scanner;

import com.monframework.annotation.MyAnnotation;
import com.monframework.annotation.MyClassAnnotation;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.util.Set;

public class AnnotationScanner {
    private static final Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage("com.monframework"))
            .setScanners(
                new MethodAnnotationsScanner(),
                new TypeAnnotationsScanner(),
                new SubTypesScanner(false)
            ));

    public static void main(String[] args) {
        System.out.println("=== DÉMARRAGE DU SCANNING ===");
        scanMethods();
        scanClasses();
        System.out.println("=== FIN ===");
    }

    public static void scanMethods() {
        try {
            Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(MyAnnotation.class);

            System.out.println("\n=== Méthodes annotées ===");
            
            if (annotatedMethods.isEmpty()) {
                System.out.println("Aucune méthode annotée trouvée !");
            } else {
                for (Method method : annotatedMethods) {
                    MyAnnotation annotation = method.getAnnotation(MyAnnotation.class);
                    System.out.println("Méthode : " + method.getDeclaringClass().getSimpleName() + 
                                     "." + method.getName() + 
                                     " - Description : " + annotation.description());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du scanning des méthodes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void scanClasses() {
        try {
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(MyClassAnnotation.class);

            System.out.println("\n=== Classes annotées ===");
            
            if (annotatedClasses.isEmpty()) {
                System.out.println("Aucune classe annotée trouvée !");
            } else {
                for (Class<?> clazz : annotatedClasses) {
                    MyClassAnnotation annotation = clazz.getAnnotation(MyClassAnnotation.class);
                    System.out.println("Classe : " + clazz.getSimpleName() + 
                                     " - Description : " + annotation.value() +
                                     " - Auteur : " + annotation.author() +
                                     " - Version : " + annotation.version());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du scanning des classes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}