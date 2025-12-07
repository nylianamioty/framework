package com.monframework.mapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLRoute {
    private final String urlPattern;
    private final Object controller;
    private final Method method;
    private final Pattern regex;
    private final String[] paramNames;
    private final String httpMethod; // GET, POST, PUT, DELETE

    public URLRoute(String urlPattern, Object controller, Method method, String httpMethod) {
        this.urlPattern = urlPattern;
        this.controller = controller;
        this.method = method;
        this.httpMethod = httpMethod.toUpperCase();
        
        // Convertir le pattern URL en regex
        String regexPattern = urlPattern;
        java.util.List<String> params = new java.util.ArrayList<>();
        
        Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = paramPattern.matcher(urlPattern);
        
        while (matcher.find()) {
            params.add(matcher.group(1));
            regexPattern = regexPattern.replace("{" + matcher.group(1) + "}", "([^/]+)");
        }
        
        this.paramNames = params.toArray(new String[0]);
        this.regex = Pattern.compile("^" + regexPattern + "$");
    }

    public boolean matches(String url, String method) {
        return regex.matcher(url).matches() && 
               this.httpMethod.equalsIgnoreCase(method);
    }

    public boolean matches(String url) {
        return regex.matcher(url).matches();
    }

    public Map<String, String> extractParams(String url) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = regex.matcher(url);
        
        if (matcher.matches()) {
            for (int i = 0; i < paramNames.length; i++) {
                params.put(paramNames[i], matcher.group(i + 1));
            }
        }
        
        return params;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public String toString() {
        return "URLRoute{" +
                "pattern='" + urlPattern + '\'' +
                ", method=" + httpMethod +
                ", controller=" + controller.getClass().getSimpleName() +
                ", handler=" + method.getName() +
                '}';
    }
}