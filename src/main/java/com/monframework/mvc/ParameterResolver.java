package com.monframework.mvc;

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
        
        List<String> allValues = new ArrayList<>();
        
        for (String value : urlParams.values()) {
            if (value != null) {
                allValues.add(value);
            }
        }
        
        Map<String, String[]> queryParams = request.getParameterMap();
        for (String[] values : queryParams.values()) {
            if (values != null && values.length > 0) {
                allValues.add(values[0]);
            }
        }
        
        System.err.println("Toutes les valeurs disponibles (ordre): " + allValues);
        
        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            
            System.err.println("Paramètre " + i + ": " + paramType.getSimpleName());
            
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
            
            // Pour les autres types, prendre dans l'ordre de la liste
            if (i < allValues.size() && allValues.get(i) != null) {
                args[i] = convertValue(allValues.get(i), paramType);
                System.err.println("  → Valeur: " + allValues.get(i) + " → " + args[i]);
            } else {
                args[i] = getDefaultValue(paramType);
                System.err.println("  → Valeur par défaut: " + args[i]);
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