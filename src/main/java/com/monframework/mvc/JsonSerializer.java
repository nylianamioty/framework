package com.monframework.mvc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class JsonSerializer {
    
    public static String toJson(Object object) {
        if (object == null) {
            return "null";
        }
        
        if (object instanceof String) {
            return "\"" + escape((String) object) + "\"";
        } else if (object instanceof Number || object instanceof Boolean) {
            return object.toString();
        } else if (object instanceof Date) {
            return "\"" + object.toString() + "\"";
        } else if (object instanceof Map) {
            return mapToJson((Map<?, ?>) object);
        } else if (object instanceof Collection) {
            return collectionToJson((Collection<?>) object);
        } else if (object.getClass().isArray()) {
            return arrayToJson(object);
        } else if (object instanceof JsonResponse) {
            return jsonResponseToJson((JsonResponse) object);
        } else {
            return objectToJson(object);
        }
    }
    
    private static String jsonResponseToJson(JsonResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        sb.append("\"status\":").append(toJson(response.getStatus())).append(",");
        sb.append("\"code\":").append(response.getCode());
        
        // Ajouter count si présent
        if (response.getCount() != null) {
            sb.append(",\"count\":").append(response.getCount());
        }
        
        // Ajouter data
        if (response.getData() != null) {
            sb.append(",\"data\":").append(toJson(response.getData()));
        } else {
            sb.append(",\"data\":null");
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    private static String escape(String str) {
        if (str == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append("\"").append(escape(entry.getKey().toString())).append("\":");
            sb.append(toJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }
    
    private static String collectionToJson(Collection<?> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Object item : collection) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(toJson(item));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String arrayToJson(Object array) {
        if (array instanceof Object[]) {
            Object[] objArray = (Object[]) array;
            List<Object> list = new ArrayList<>();
            for (Object obj : objArray) {
                list.add(obj);
            }
            return collectionToJson(list);
        } else {
            // Tableau de primitives
            int length = java.lang.reflect.Array.getLength(array);
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                list.add(java.lang.reflect.Array.get(array, i));
            }
            return collectionToJson(list);
        }
    }
    
    private static String objectToJson(Object obj) {
        try {
            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();
            Method[] methods = clazz.getDeclaredMethods();
            
            Map<String, Object> properties = new java.util.HashMap<>();
            
            // Chercher les getters
            for (Method method : methods) {
                String methodName = method.getName();
                if ((methodName.startsWith("get") || methodName.startsWith("is")) 
                    && method.getParameterCount() == 0 
                    && !methodName.equals("getClass")) {
                    
                    String propertyName;
                    if (methodName.startsWith("get")) {
                        propertyName = methodName.substring(3);
                    } else {
                        propertyName = methodName.substring(2);
                    }
                    
                    if (!propertyName.isEmpty()) {
                        propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
                        try {
                            Object value = method.invoke(obj);
                            properties.put(propertyName, value);
                        } catch (Exception e) {
                            // Ignorer
                        }
                    }
                }
            }
            
            return mapToJson(properties);
        } catch (Exception e) {
            return "{}";
        }
    }
}