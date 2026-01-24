package com.monframework.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteRegistry {
    private final List<URLRoute> routes = new ArrayList<>();

    public void registerRoute(URLRoute route) {
        routes.add(route);
    }

    public void registerRoutes(List<URLRoute> routes) {
        this.routes.addAll(routes);
    }

    
    public URLRoute findRoute(String url, String method) {
        for (URLRoute route : routes) {
            if (route.matches(url) && route.getHttpMethod().equalsIgnoreCase(method)) {
                return route;
            }
        }
        return null;
    }

    
    public URLRoute findRoute(String url) {
        for (URLRoute route : routes) {
            if (route.matches(url)) {
                return route;
            }
        }
        return null;
    }

    public Map<String, String> extractParams(URLRoute route, String url) {
        return route.extractParams(url);
    }

    public List<URLRoute> getAllRoutes() {
        return new ArrayList<>(routes);
    }

    public int size() {
        return routes.size();
    }

    public void printRoutes() {
        System.out.println("\n║                         ROUTES ENREGISTRÉES                                ║");
        
        if (routes.isEmpty()) {
            System.out.println("  Aucune route enregistrée");
        } else {
            for (URLRoute route : routes) {
                String className = route.getController().getClass().getSimpleName();
                String methodName = route.getMethod().getName();
                String urlPattern = route.getUrlPattern();
                String httpMethod = route.getHttpMethod();
                
                System.out.println("\n   URL: " + urlPattern + " [" + httpMethod + "]");
                System.out.println("     ├─ Classe: " + className);
                System.out.println("     └─ Méthode: " + methodName + "()");
            }
            
            System.out.println("\n  Total: " + routes.size() + " route(s) chargée(s) avec succès");
        }
    }
}