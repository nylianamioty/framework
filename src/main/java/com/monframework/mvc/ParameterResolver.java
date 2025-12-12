package com.monframework.mvc;

import com.monframework.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.HashMap;
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
        
        // Créer une Map combinée avec TOUS les paramètres
        Map<String, Object> allParamsMap = createAllParamsMap(request, urlParams);
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            
            System.err.println("Paramètre " + i + ": " + paramType.getSimpleName());
            
            // 1. Types spéciaux
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
            // 2. Si c'est une Map<String, Object> - injection complète
            else if (Map.class.isAssignableFrom(paramType)) {
                args[i] = allParamsMap;
                System.err.println("  → Map<String, Object> injectée (" + allParamsMap.size() + " éléments)");
                continue;
            }
            
            // 3. Si le paramètre a l'annotation @RequestParam
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
            // 4. Sans annotation - injection par ORDRE
            else {
                List<String> allValues = new ArrayList<>();
                allValues.addAll(urlParams.values());
                
                Map<String, String[]> queryParams = request.getParameterMap();
                for (String[] values : queryParams.values()) {
                    if (values != null && values.length > 0) {
                        allValues.add(values[0]);
                    }
                }
                
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
    
    private static Map<String, Object> createAllParamsMap(HttpServletRequest request, Map<String, String> urlParams) {
        Map<String, Object> allParams = new HashMap<>();
        
        // 1. Ajouter tous les paramètres d'URL
        allParams.putAll(urlParams);
        
        // 2. Ajouter tous les paramètres de requête
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                // Si un seul paramètre, le stocker comme String
                if (entry.getValue().length == 1) {
                    allParams.put(entry.getKey(), entry.getValue()[0]);
                } else {
                    // Sinon garder le tableau
                    allParams.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // 3. Ajouter tous les attributs de la requête
        var attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            allParams.put(name, request.getAttribute(name));
        }
        
        System.err.println("Map complète créée: " + allParams.keySet());
        return allParams;
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