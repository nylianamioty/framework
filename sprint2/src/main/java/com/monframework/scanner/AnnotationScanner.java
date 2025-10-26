package com.monframework.scanner;

import com.monframework.annotation.MyAnnotation;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.Set;

public class AnnotationScanner {

    public static void main(String[] args) {
        // Crée un objet Reflections pour scanner le package com.monframework
        Reflections reflections = new Reflections("com.monframework");

        // Recherche toutes les méthodes annotées avec MyAnnotation
        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(MyAnnotation.class);

        // Affiche les méthodes trouvées
        for (Method method : annotatedMethods) {
            MyAnnotation annotation = method.getAnnotation(MyAnnotation.class);
            System.out.println("Méthode : " + method.getName() + " - Description : " + annotation.description());
        }
    }
}
