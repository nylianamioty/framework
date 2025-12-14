package com.monframework;

import com.monframework.mapping.ControllerScanner;
import com.monframework.mapping.RouteRegistry;
import com.monframework.mapping.URLRoute;
import com.monframework.mvc.ModelView;
import com.monframework.annotation.Json;
import com.monframework.mvc.JsonResponse;
import com.monframework.mvc.JsonSerializer;
import com.monframework.mvc.ModelAndView; // AJOUT
import com.monframework.mvc.ParameterResolver;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class FrontServlet extends HttpServlet {

    private RequestDispatcher defaultDispatcher;
    private RouteRegistry routeRegistry;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        routeRegistry = new RouteRegistry();

        String basePackage = config.getInitParameter("controller-package");
        if (basePackage == null || basePackage.isEmpty()) {
            basePackage = "com.giga.springlab.controller";
        }

        System.out.println("=== Initialisation du FrontServlet ===");
        System.out.println("Scan du package: " + basePackage);

        List<URLRoute> routes = ControllerScanner.scanPackage(basePackage);
        routeRegistry.registerRoutes(routes);

        routeRegistry.printRoutes();
        System.out.println("=== FrontServlet initialisé ===");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String httpMethod = req.getMethod().toUpperCase();
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            URLRoute route = routeRegistry.findRoute(path);
            if (route != null) {
                // Vérifier si la méthode HTTP est autorisée
                if (isHttpMethodAllowed(route, httpMethod)) {
                    invokeController(route, path, req, res);
                } else {
                    sendMethodNotAllowed(res, httpMethod, route.getHttpMethod());
                }
            } else {
                customServe(req, res);
            }
        }
    }
    
    private boolean isHttpMethodAllowed(URLRoute route, String requestMethod) {
        String routeMethod = route.getHttpMethod().toUpperCase();
        
        if (routeMethod == null || routeMethod.isEmpty() || routeMethod.equals("GET")) {
            // Accepter GET, POST, etc. pour l'ancien système
            return true; // Tout est autorisé pour la compatibilité
        }
        
        return routeMethod.equals(requestMethod);
    }
    
    private void sendMethodNotAllowed(HttpServletResponse res, String requestMethod, String allowedMethod) 
            throws IOException {
        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        res.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = res.getWriter()) {
            out.println("<html><head><title>405 - Méthode non autorisée</title></head><body>");
            out.println("<h1>405 - Méthode non autorisée</h1>");
            out.println("<p>La méthode HTTP " + requestMethod + " n'est pas autorisée pour cette ressource.</p>");
            out.println("<p>Méthode autorisée: " + allowedMethod + "</p>");
            out.println("</body></html>");
        }
    }

private void handleJsonResponse(Object result, HttpServletResponse res) throws IOException {
    System.out.println("=== HANDLE JSON RESPONSE ===");
    System.out.println("Type du résultat: " + (result != null ? result.getClass().getName() : "null"));
    
    JsonResponse jsonResponse;
    
    // Si le résultat est déjà un JsonResponse, l'utiliser directement
    if (result instanceof JsonResponse) {
        System.out.println("Résultat est déjà un JsonResponse");
        jsonResponse = (JsonResponse) result;
    } 
    // Sinon créer un JsonResponse standard
    else {
        System.out.println("Création d'un JsonResponse standard");
        jsonResponse = new JsonResponse();
        jsonResponse.setStatus("success");
        jsonResponse.setCode(200);
        jsonResponse.setData(result);
        
        // Calculer count si c'est une collection/tableau
        if (result instanceof java.util.Collection) {
            jsonResponse.setCount(((java.util.Collection<?>) result).size());
            System.out.println("C'est une Collection, count: " + jsonResponse.getCount());
        } else if (result != null && result.getClass().isArray()) {
            jsonResponse.setCount(java.lang.reflect.Array.getLength(result));
            System.out.println("C'est un tableau, count: " + jsonResponse.getCount());
        } else if (result instanceof java.util.Map) {
            jsonResponse.setCount(((java.util.Map<?, ?>) result).size());
            System.out.println("C'est une Map, count: " + jsonResponse.getCount());
        }
    }
    
    // Sérialiser en JSON
    String json = JsonSerializer.toJson(jsonResponse);
    
    System.out.println("JSON généré: " + json);
    
    // Écrire la réponse
    res.setContentType("application/json;charset=UTF-8");
    try (PrintWriter out = res.getWriter()) {
        out.print(json);
        System.out.println("JSON envoyé au client");
    } catch (IOException e) {
        System.err.println("Erreur d'écriture JSON: " + e.getMessage());
        throw e;
    }
}

private void handleJsonError(Exception e, HttpServletResponse res) throws IOException {
    System.err.println("=== HANDLE JSON ERROR ===");
    
    JsonResponse jsonResponse = new JsonResponse();
    jsonResponse.setStatus("error");
    jsonResponse.setCode(500);
    
    java.util.Map<String, String> errorDetails = new java.util.HashMap<>();
    errorDetails.put("message", e.getMessage());
    errorDetails.put("type", e.getClass().getName());
    
    jsonResponse.setData(errorDetails);
    
    String json = JsonSerializer.toJson(jsonResponse);
    
    System.err.println("JSON d'erreur: " + json);
    
    res.setStatus(500);
    res.setContentType("application/json;charset=UTF-8");
    try (PrintWriter out = res.getWriter()) {
        out.print(json);
    }
}

private void invokeController(URLRoute route, String path, HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
    Method method = null;
    try {
        System.out.println("=== DEBUG FrontServlet ===");
        System.out.println("URL demandée: " + path);
        System.out.println("Méthode HTTP: " + req.getMethod());
        System.out.println("Route trouvée: " + route.getUrlPattern());
        System.out.println("Méthode route: " + route.getHttpMethod());
        System.out.println("Contrôleur: " + route.getController().getClass().getName());
        System.out.println("Méthode: " + route.getMethod().getName());
        
        Map<String, String> urlParams = route.extractParams(path);
        System.out.println("Paramètres URL extraits: " + urlParams);
        System.out.println("Paramètres Query: " + req.getParameterMap());
        
        method = route.getMethod();
        Object controller = route.getController();
        Object result;

        Object[] args = ParameterResolver.resolveParameters(method, req, urlParams);
        
        // Injecter HttpServletResponse si nécessaire
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] == HttpServletResponse.class && args[i] == null) {
                args[i] = res;
            }
        }
        
        System.out.println("Paramètres résolus: " + java.util.Arrays.toString(args));
        
        // Appel de la méthode avec les paramètres résolus
        result = method.invoke(controller, args);

        // DEBUG: Vérifier si la méthode a l'annotation @Json
        boolean hasJsonAnnotation = method.isAnnotationPresent(Json.class);
        System.out.println("Méthode a @Json ? " + hasJsonAnnotation);
        
        // Si la méthode est annotée @Json, traiter comme réponse JSON
        if (hasJsonAnnotation) {
            System.out.println("Traitement comme réponse JSON");
            handleJsonResponse(result, res);
            return;
        }

        // Traiter le retour selon le type (SEULEMENT si pas déjà géré dans la méthode)
        boolean methodHandledResponse = false;
        for (Object arg : args) {
            if (arg == res) {
                methodHandledResponse = true;
                break;
            }
        }
        
        if (!methodHandledResponse && result instanceof String) {
            // Retour String -> PrintWriter direct
            String responseString = (String) result;
            System.out.println("Retour String: " + responseString);
            res.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = res.getWriter()) {
                out.print(responseString);
            }
            
        } else if (!methodHandledResponse && result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            String viewName = mv.getView();
            System.out.println("Retour ModelView -> JSP: " + viewName);
            
            Map<String, Object> data = mv.getData();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
                System.out.println("Donnée JSP: " + entry.getKey() + " = " + entry.getValue());
            }
            
            urlParams.forEach(req::setAttribute);
            
            RequestDispatcher dispatcher = req.getRequestDispatcher("/" + viewName);
            if (dispatcher != null) {
                dispatcher.forward(req, res);
                System.out.println("Forward réussi vers: " + viewName);
            } else {
                throw new ServletException("JSP non trouvée: " + viewName);
            }
        } else if (!methodHandledResponse && result instanceof ModelAndView) {
            ModelAndView mv = (ModelAndView) result;
            String viewName = mv.getView();
            System.out.println("Retour ModelAndView -> JSP: " + viewName);
            
            // Transférer le modèle au request
            Map<String, Object> model = mv.getModel();
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
                System.out.println("Donnée JSP: " + entry.getKey() + " = " + entry.getValue());
            }
            
            urlParams.forEach(req::setAttribute);
            
            // Forward vers la JSP
            RequestDispatcher dispatcher = req.getRequestDispatcher("/" + viewName);
            if (dispatcher != null) {
                dispatcher.forward(req, res);
                System.out.println("Forward réussi vers: " + viewName);
            } else {
                throw new ServletException("JSP non trouvée: " + viewName);
            }
        }
        // Si la méthode a géré la réponse elle-même (avec HttpServletResponse), on ne fait rien

    } catch (Exception e) {
        System.err.println("ERREUR lors de l'invocation du contrôleur: " + e.getMessage());
        e.printStackTrace();

        // Si la méthode avait l'annotation @Json, renvoyer une erreur JSON
        if (method != null && method.isAnnotationPresent(Json.class)) {
            handleJsonError(e, res);
        } else {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = res.getWriter()) {
                out.println("<html><head><title>Erreur Serveur</title></head><body>");
                out.println("<h1>Erreur 500 - Erreur Interne du Serveur</h1>");
                out.println("<p>Une erreur s'est produite lors du traitement de la requête.</p>");
                out.println("<pre>URL: " + path + "</pre>");
                out.println("<pre>Erreur: " + e.getMessage() + "</pre>");
                out.println("</body></html>");
            }
        }
    }
}
   
 
    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String uri = req.getRequestURI();
        String responseBody =
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<title>404 - Url tsy fantatra</title>" +
            "</head>" +
            "<body>" +
            "<div>" +
            "<h1>404</h1>" +
            "<div>Url tsy fantatra</div>" +
            "<div>L'URL demandée n'a pas été trouvée</div>" +
            "<div>" + uri + "</div>" +
            "<p><a href='" + req.getContextPath() + "/home'>← Retour à l'accueil</a></p>" +
            "</div>" +
            "</body>" +
            "</html>";

        res.setContentType("text/html;charset=UTF-8");
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        try (PrintWriter out = res.getWriter()) {
            out.println(responseBody);
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}