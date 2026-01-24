package com.monframework.mapping;

import com.monframework.annotation.Controller;
import com.monframework.annotation.RequestMapping;
import com.monframework.annotation.GetMapping;
import com.monframework.annotation.PostMapping;
import com.monframework.annotation.PutMapping;
import com.monframework.annotation.DeleteMapping;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ControllerScanner {

    public static List<URLRoute> scanPackage(String packageName) {
        List<URLRoute> routes = new ArrayList<>();
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            
            for (File directory : dirs) {
                routes.addAll(findControllers(directory, packageName));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du scan du package " + packageName + ": " + e.getMessage());
        }
        
        return routes;
    }

    private static List<URLRoute> findControllers(File directory, String packageName) {
        List<URLRoute> routes = new ArrayList<>();
        
        if (!directory.exists()) {
            return routes;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return routes;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                routes.addAll(findControllers(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                routes.addAll(scanClass(className));
            }
        }
        
        return routes;
    }

   private static List<URLRoute> scanClass(String className) {
    List<URLRoute> routes = new ArrayList<>();
    
    try {
        Class<?> clazz = Class.forName(className);
        
        if (!clazz.isAnnotationPresent(com.monframework.annotation.Controller.class)) {
            return routes;
        }
        
        System.out.println("Contrôleur trouvé: " + clazz.getSimpleName());
        
        Object controller = clazz.getDeclaredConstructor().newInstance();
        
        if (controller instanceof com.monframework.controller.Controller) {
            ((com.monframework.controller.Controller) controller).init();
        }
        
        // Scanner les méthodes
        for (Method method : clazz.getDeclaredMethods()) {
            String urlPattern = null;
            String httpMethod = "GET";
            
            // Vérifier les nouvelles annotations HTTP
            if (method.isAnnotationPresent(com.monframework.annotation.RequestMapping.class)) {
                com.monframework.annotation.RequestMapping annotation = 
                    method.getAnnotation(com.monframework.annotation.RequestMapping.class);
                urlPattern = annotation.value();
                httpMethod = annotation.method().toUpperCase();
                System.out.println("  ├─ @RequestMapping: " + httpMethod + " " + urlPattern);
            }
            else if (method.isAnnotationPresent(com.monframework.annotation.GetMapping.class)) {
                com.monframework.annotation.GetMapping annotation = 
                    method.getAnnotation(com.monframework.annotation.GetMapping.class);
                urlPattern = annotation.value();
                httpMethod = "GET";
                System.out.println("  ├─ @GetMapping: " + urlPattern + " [GET]");
            }
            else if (method.isAnnotationPresent(com.monframework.annotation.PostMapping.class)) {
                com.monframework.annotation.PostMapping annotation = 
                    method.getAnnotation(com.monframework.annotation.PostMapping.class);
                urlPattern = annotation.value();
                httpMethod = "POST";
                System.out.println("  ├─ @PostMapping: " + urlPattern + " [POST]");
            }
            else if (method.isAnnotationPresent(com.monframework.annotation.PutMapping.class)) {
                com.monframework.annotation.PutMapping annotation = 
                    method.getAnnotation(com.monframework.annotation.PutMapping.class);
                urlPattern = annotation.value();
                httpMethod = "PUT";
                System.out.println("  ├─ @PutMapping: " + urlPattern);
            }
            else if (method.isAnnotationPresent(com.monframework.annotation.DeleteMapping.class)) {
                com.monframework.annotation.DeleteMapping annotation = 
                    method.getAnnotation(com.monframework.annotation.DeleteMapping.class);
                urlPattern = annotation.value();
                httpMethod = "DELETE";
                System.out.println("  ├─ @DeleteMapping: " + urlPattern);
            }
            // Compatibilité avec l'ancienne annotation @URLMapping
            else if (method.isAnnotationPresent(com.monframework.annotation.URLMapping.class)) {
                com.monframework.annotation.URLMapping annotation = 
                    method.getAnnotation(com.monframework.annotation.URLMapping.class);
                urlPattern = annotation.value();
                httpMethod = "GET";
                System.out.println("  ├─ @URLMapping (ancien): " + urlPattern);
            }
            
            if (urlPattern != null) {
                URLRoute route = new URLRoute(urlPattern, controller, method, httpMethod);
                routes.add(route);
                System.out.println("  └─ Route ajoutée: " + httpMethod + " " + urlPattern);
            } else {
                System.out.println("  └─ Méthode sans mapping: " + method.getName());
            }
        }
        
    } catch (Exception e) {
        System.err.println("Erreur lors du scan de la classe " + className + ": " + e.getMessage());
        e.printStackTrace();
    }
    
    return routes;
}
}