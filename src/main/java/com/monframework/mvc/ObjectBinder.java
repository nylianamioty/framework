package com.monframework.mvc;

import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjectBinder {
    
    public static Object bindObject(Class<?> targetClass, Map<String, Object> allParams, String prefix) {
        try {
            Object instance = targetClass.getDeclaredConstructor().newInstance();
            
            for (Field field : targetClass.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                String paramName = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
                
                // Gérer les tableaux
                if (field.getType().isArray()) {
                    bindArray(field, instance, allParams, paramName);
                }
                // Gérer les objets imbriqués
                else if (!isBasicType(field.getType())) {
                    Object nestedObject = bindObject(field.getType(), allParams, paramName);
                    field.set(instance, nestedObject);
                }
                // Gérer les types basiques
                else {
                    if (allParams.containsKey(paramName)) {
                        Object value = allParams.get(paramName);
                        field.set(instance, convertValue(value, field.getType()));
                    }
                }
            }
            
            return instance;
        } catch (Exception e) {
            System.err.println("Erreur lors du binding de " + targetClass.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private static void bindArray(Field field, Object instance, Map<String, Object> allParams, String baseName) 
            throws Exception {
        Class<?> componentType = field.getType().getComponentType();
        List<Object> list = new ArrayList<>();
        
        // Compter combien d'éléments dans le tableau
        int index = 0;
        while (true) {
            boolean found = false;
            
            // Vérifier pour chaque paramètre de tableau
            for (Map.Entry<String, Object> entry : allParams.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(baseName + "[" + index + "]")) {
                    found = true;
                    
                    // Si c'est un objet complexe dans le tableau
                    if (!isBasicType(componentType)) {
                        // Extraire le suffixe après l'indice
                        String suffix = key.substring((baseName + "[" + index + "]").length());
                        if (suffix.startsWith(".")) {
                            suffix = suffix.substring(1);
                        }
                        
                        // Créer l'objet du tableau
                        if (index >= list.size()) {
                            list.add(componentType.getDeclaredConstructor().newInstance());
                        }
                        
                        Object arrayElement = list.get(index);
                        setFieldValue(arrayElement, suffix, entry.getValue(), componentType);
                    } else {
                        // Type basique direct
                        if (index >= list.size()) {
                            list.add(convertValue(entry.getValue(), componentType));
                        }
                    }
                }
            }
            
            if (!found) break;
            index++;
        }
        
        if (!list.isEmpty()) {
            Object array = Array.newInstance(componentType, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            field.set(instance, array);
        }
    }
    
    private static void setFieldValue(Object obj, String fieldPath, Object value, Class<?> targetClass) 
            throws Exception {
        String[] parts = fieldPath.split("\\.");
        Object current = obj;
        
        for (int i = 0; i < parts.length - 1; i++) {
            Field field = targetClass.getDeclaredField(parts[i]);
            field.setAccessible(true);
            
            if (field.get(current) == null) {
                field.set(current, field.getType().getDeclaredConstructor().newInstance());
            }
            current = field.get(current);
            targetClass = field.getType();
        }
        
        Field lastField = targetClass.getDeclaredField(parts[parts.length - 1]);
        lastField.setAccessible(true);
        lastField.set(current, convertValue(value, lastField.getType()));
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
    
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        String stringValue = value.toString();
        try {
            if (targetType == String.class) {
                return stringValue;
            } else if (targetType == int.class || targetType == Integer.class) {
                return stringValue.isEmpty() ? 0 : Integer.parseInt(stringValue);
            } else if (targetType == long.class || targetType == Long.class) {
                return stringValue.isEmpty() ? 0L : Long.parseLong(stringValue);
            } else if (targetType == double.class || targetType == Double.class) {
                return stringValue.isEmpty() ? 0.0 : Double.parseDouble(stringValue);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(stringValue);
            } else if (targetType == float.class || targetType == Float.class) {
                return stringValue.isEmpty() ? 0.0f : Float.parseFloat(stringValue);
            }
        } catch (NumberFormatException e) {
            System.err.println("Erreur conversion: " + stringValue + " vers " + targetType);
        }
        
        return null;
    }
}