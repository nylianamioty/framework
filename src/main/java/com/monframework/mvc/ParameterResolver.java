package com.monframework.mvc;

import com.monframework.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ParameterResolver {
    
    public static Object[] resolveParameters(Method method, HttpServletRequest request, Map<String, String> urlParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        System.err.println("=== DEBUG ParameterResolver ===");
        System.err.println("Méthode: " + method.getName());
        System.err.println("URL Params: " + urlParams);
        System.err.println("Query Params: " + request.getParameterMap().keySet());
        
        // Combiner toutes les valeurs disponibles dans l'ordre
        List<String> allValues = new ArrayList<>();
        
        // 1. D'abord les valeurs d'URL
        allValues.addAll(urlParams.values());
        
        // 2. Ensuite les valeurs de requête  
        Map<String, String[]> queryParams = request.getParameterMap();
        for (String[] values : queryParams.values()) {
            if (values != null && values.length > 0) {
                allValues.add(values[0]);
            }
        }
        
        System.err.println("Toutes les valeurs (ordre): " + allValues);
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            
            System.err.println("Paramètre " + i + ": " + paramType.getSimpleName());
            
            // Types spéciaux
            if (paramType == HttpServletRequest.class) {
                args[i] = request;
                System.err.println("  → HttpServletRequest");
                continue;
            }
            else if (paramType == HttpServletResponse.class) {
                args[i] = null;
                System.err.println("  → HttpServletResponse (réservé)");
                continue;
            }
            
            // Avec @RequestParam - chercher par nom exact
            if (requestParam != null) {
                String paramName = requestParam.value();
                String paramValue = null;
                
                if (urlParams.containsKey(paramName)) {
                    paramValue = urlParams.get(paramName);
                    System.err.println("  → @RequestParam trouvé dans URL: " + paramValue);
                }
                else if (request.getParameter(paramName) != null) {
                    paramValue = request.getParameter(paramName);
                    System.err.println("  → @RequestParam trouvé dans Query: " + paramValue);
                }
                else if (requestParam.required()) {
                    throw new RuntimeException("Paramètre requis manquant: " + paramName);
                }
                else if (!requestParam.defaultValue().isEmpty()) {
                    paramValue = requestParam.defaultValue();
                    System.err.println("  → @RequestParam valeur par défaut: " + paramValue);
                }
                
                args[i] = convertValue(paramValue, paramType);
            }
            // Sans annotation - injection par ORDRE
            else {
                if (i < allValues.size() && allValues.get(i) != null) {
                    args[i] = convertValue(allValues.get(i), paramType);
                    System.err.println("  → Injection par ordre: " + allValues.get(i) + " → " + args[i]);
                } else {
                    args[i] = getDefaultValue(paramType);
                    System.err.println("  → Valeur par défaut: " + args[i]);
                }
            }
        }
        
        System.err.println("Paramètres résolus: " + java.util.Arrays.toString(args));
        return args;
    }
    
    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;
        
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return value.isEmpty() ? 0 : Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return value.isEmpty() ? 0L : Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return value.isEmpty() ? 0.0 : Double.parseDouble(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
        } catch (NumberFormatException e) {
            System.err.println("Erreur conversion: " + value + " vers " + targetType);
            return getDefaultValue(targetType);
        }
        
        return value;
    }
    
    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        return null;
    }
}