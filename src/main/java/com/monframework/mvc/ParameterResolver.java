package com.monframework.mvc;

import com.monframework.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
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

         
            // 3. Si c'est un objet complexe (pas un type basique) - faire le binding
            else if (!isBasicType(paramType)) {
                args[i] = ObjectBinder.bindObject(paramType, allParamsMap, "");
                System.err.println("  → Objet " + paramType.getSimpleName() + " bindé: " + args[i]);
                continue;
            }
            
            // 4. Si le paramètre a l'annotation @RequestParam
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
               else if (paramType == UploadedFile.class) {
                String paramName = getParameterName(param, requestParam);
                // Récupérer les fichiers uploadés
                Map<String, UploadedFile> files = (Map<String, UploadedFile>) request.getAttribute("UPLOADED_FILES");
                if (files != null && files.containsKey(paramName)) {
                    args[i] = files.get(paramName);
                    System.err.println("  → Fichier uploadé: " + args[i]);
                } else {
                    args[i] = null;
                    System.err.println("  → Aucun fichier uploadé pour: " + paramName);
                }
                continue;
            }
            // 5. Sans annotation - injection par ORDRE
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
    
        public static UploadedFile handleFileUpload(Part part) throws IOException {
        if (part == null || part.getSize() == 0) {
            return null;
        }
        
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setName(getFileName(part));
        uploadedFile.setContentType(part.getContentType());
        uploadedFile.setSize(part.getSize());
        
        // Lire le contenu du fichier
        byte[] content = new byte[(int) part.getSize()];
        try (InputStream inputStream = part.getInputStream()) {
            inputStream.read(content);
        }
        uploadedFile.setContent(content);
        
        return uploadedFile;
    }

    private static String getParameterName(Parameter param, RequestParam requestParam) {
    if (requestParam != null) {
        return requestParam.value(); // Nom spécifié dans @RequestParam
    }
    
    // Essayer de récupérer le nom réel du paramètre
    try {
        if (param.isNamePresent()) {
            return param.getName();
        }
    } catch (Exception e) {
        // Ignorer si pas disponible
    }
    
    return null;
}
    private static String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        String[] items = contentDisposition.split(";");
        for (String item : items) {
            if (item.trim().startsWith("filename")) {
                return item.substring(item.indexOf("=") + 2, item.length() - 1);
            }
        }
        return "";
    }
    private static boolean isBasicType(Class<?> type) {
        return type == String.class || 
               type == int.class || type == Integer.class ||
               type == long.class || type == Long.class ||
               type == double.class || type == Double.class ||
               type == boolean.class || type == Boolean.class ||
               type == float.class || type == Float.class ||
               type == byte.class || type == Byte.class ||
               type == short.class || type == Short.class ||
               type == char.class || type == Character.class;
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
            } else if (targetType == float.class || targetType == Float.class) {
                return value.isEmpty() ? 0.0f : Float.parseFloat(value);
            }
        } catch (NumberFormatException e) {
            System.err.println("Erreur conversion: " + value + " vers " + targetType);
            return getDefaultValue(targetType);
        }
        
        return null;
    }
    
    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        if (type == float.class) return 0.0f;
        return null;
    }
}