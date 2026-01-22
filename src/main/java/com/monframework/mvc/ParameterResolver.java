package com.monframework.mvc;

import com.monframework.annotation.RequestParam;
import com.monframework.annotation.SessionAttribute;
import com.monframework.annotation.RemoveSessionAttribute;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
    
    public static Object[] resolveParameters(Method method, HttpServletRequest request, 
                                           HttpServletResponse response, Map<String, String> urlParams) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        System.err.println("=== DEBUG ParameterResolver ===");
        System.err.println("Méthode: " + method.getName());
        
        // Créer une Map combinée avec TOUS les paramètres
        Map<String, Object> allParamsMap = createAllParamsMap(request, urlParams);
        
        // Stocker la session pour utilisation ultérieure
        HttpSession session = request.getSession(false);
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            SessionAttribute sessionAttr = param.getAnnotation(SessionAttribute.class);
            
            System.err.println("Paramètre " + i + ": " + paramType.getSimpleName() + 
                             (requestParam != null ? " @RequestParam(\"" + requestParam.value() + "\")" : "") +
                             (sessionAttr != null ? " @SessionAttribute(\"" + sessionAttr.value() + "\")" : ""));
            
            // 1. Types spéciaux
            if (paramType == HttpServletRequest.class) {
                args[i] = request;
                System.err.println("  → HttpServletRequest");
                continue;
            }
            else if (paramType == HttpServletResponse.class) {
                args[i] = response;
                System.err.println("  → HttpServletResponse");
                continue;
            }
            else if (paramType == HttpSession.class) {
                // Toujours retourner une session (créer si nécessaire)
                args[i] = request.getSession(true);
                System.err.println("  → HttpSession");
                continue;
            }
            // 2. Si c'est une Map<String, Object> - injection complète
            else if (Map.class.isAssignableFrom(paramType)) {
                args[i] = allParamsMap;
                System.err.println("  → Map<String, Object> injectée (" + allParamsMap.size() + " éléments)");
                continue;
            }
            // 3. Si le paramètre a l'annotation @SessionAttribute
            else if (sessionAttr != null) {
                String attrName = sessionAttr.value();
                
                // IMPORTANT: Stratégie pour déterminer si on doit LIRE ou ÉCRIRE
                // 1. Vérifier si ce paramètre existe dans les paramètres de la requête
                //    (cela signifie qu'on veut ÉCRIRE dans la session)
                // 2. Sinon, LIRE depuis la session
                
                boolean hasValueInRequest = allParamsMap.containsKey(attrName);
                
                if (hasValueInRequest) {
                    // ÉCRITURE: On a une valeur dans la requête, on l'écrit dans la session
                    Object value = allParamsMap.get(attrName);
                    Object convertedValue = convertValueBasedOnType(value, paramType);
                    
                    // Créer la session si nécessaire
                    if (session == null) {
                        session = request.getSession(true);
                    }
                    
                    session.setAttribute(attrName, convertedValue);
                    args[i] = convertedValue;
                    System.err.println("  → ÉCRITURE session '" + attrName + "': " + args[i]);
                } else {
                    // LECTURE: On veut lire depuis la session
                    if (session == null && sessionAttr.required()) {
                        throw new RuntimeException("Attribut de session requis manquant: " + attrName);
                    }
                    
                    if (session != null) {
                        Object attrValue = session.getAttribute(attrName);
                        if (attrValue != null) {
                            args[i] = convertSessionValue(attrValue, paramType);
                            System.err.println("  → LECTURE session '" + attrName + "': " + args[i]);
                        } else if (sessionAttr.required()) {
                            throw new RuntimeException("Attribut de session requis manquant: " + attrName);
                        } else {
                            args[i] = null;
                            System.err.println("  → Attribut de session '" + attrName + "' non trouvé (non requis)");
                        }
                    } else {
                        args[i] = null;
                        System.err.println("  → Aucune session pour lire '" + attrName + "'");
                    }
                }
                continue;
            }
            // 4. Si c'est un objet complexe (pas un type basique) - faire le binding
            else if (!isBasicType(paramType)) {
                args[i] = ObjectBinder.bindObject(paramType, allParamsMap, "");
                System.err.println("  → Objet " + paramType.getSimpleName() + " bindé: " + args[i]);
                continue;
            }
            
            // 5. Si le paramètre a l'annotation @RequestParam
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
            // 6. Sans annotation - injection par ORDRE
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
    
    // Nouvelle méthode pour convertir les valeurs des paramètres selon le type
    private static Object convertValueBasedOnType(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value.toString());
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value.toString());
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value.toString());
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value.toString());
        }
        
        return value;
    }
    
    // Convertir une valeur de session au type attendu
    private static Object convertSessionValue(Object sessionValue, Class<?> targetType) {
        if (sessionValue == null) return null;
        
        // Si les types correspondent directement
        if (targetType.isInstance(sessionValue)) {
            return sessionValue;
        }
        
        // Sinon, essayer une conversion
        return convertValueBasedOnType(sessionValue, targetType);
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