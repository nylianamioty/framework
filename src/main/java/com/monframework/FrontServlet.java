package com.monframework;

import com.monframework.mapping.ControllerScanner;
import com.monframework.mapping.RouteRegistry;
import com.monframework.mapping.URLRoute;
import com.monframework.mvc.ModelView;
import com.monframework.mvc.ModelAndView;
import com.monframework.mvc.ParameterResolver;
import com.monframework.mvc.UploadedFile;
import com.monframework.annotation.RemoveSessionAttribute;
import com.monframework.security.SecurityManager;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@MultipartConfig(
    maxFileSize = 1024 * 1024 * 5,      // 5 MB max par fichier
    maxRequestSize = 1024 * 1024 * 10   // 10 MB max total
)
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
        String contentType = req.getContentType();

        System.err.println("=== SERVICE ===");
        System.err.println("Path: " + path);
        System.err.println("Method: " + httpMethod);
        System.err.println("Context Path: " + req.getContextPath());
        System.err.println("Content-Type: " + contentType);

        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
        System.err.println("  -> Requête multipart détectée");
        handleMultipartRequest(req, res);
        return;
    }
        if (resourceExists) {
            defaultServe(req, res);
        } else {
            URLRoute route = routeRegistry.findRoute(path, httpMethod);
            
            if (route != null) {
                System.err.println("Route trouvée: " + route.getUrlPattern() + " [" + route.getHttpMethod() + "]");
                invokeController(route, path, req, res);
            } else {
                URLRoute anyMethodRoute = routeRegistry.findRoute(path);
                if (anyMethodRoute != null) {
                    System.err.println("Méthode non autorisée! Route: " + anyMethodRoute.getHttpMethod() + ", Requête: " + httpMethod);
                    sendMethodNotAllowed(res, httpMethod, anyMethodRoute.getHttpMethod());
                } else {
                    System.err.println("Aucune route trouvée pour: " + path);
                    customServe(req, res);
                }
            }
        }
    }

    private void handleMultipartRequest(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException {
    try {
        System.err.println("=== HANDLE MULTIPART REQUEST ===");
        System.err.println("Content-Type: " + req.getContentType());
        
        // Utiliser la Part API de Servlet 3.0
        Collection<Part> parts = req.getParts();
        System.err.println("Nombre de parts: " + parts.size());
        
        Map<String, String[]> parameters = new HashMap<>();
        Map<String, UploadedFile> files = new HashMap<>();
        
        // Séparer les paramètres normaux des fichiers
        for (Part part : parts) {
            System.err.println("Part: " + part.getName() + " - " + part.getContentType());
            if (part.getContentType() != null) {
                // C'est un fichier
                System.err.println("  -> Traitement comme fichier");
                UploadedFile uploadedFile = new UploadedFile(part);
                if (uploadedFile != null) {
                    files.put(part.getName(), uploadedFile);
                    System.err.println("  -> Fichier enregistré: " + uploadedFile.getName());
                }
            } else {
                // C'est un paramètre normal
                System.err.println("  -> Traitement comme paramètre");
                String value = readPartValue(part);
                parameters.put(part.getName(), new String[]{value});
                System.err.println("  -> Valeur: " + value);
            }
        }
        
        // Stocker dans la requête
        req.setAttribute("UPLOADED_FILES", files);
        
        // Continuer avec le traitement normal
        String path = req.getRequestURI().substring(req.getContextPath().length());
        System.err.println("Path pour routing: " + path);
        URLRoute route = routeRegistry.findRoute(path);
        
        if (route != null) {
            invokeController(route, path, req, res);
        } else {
            customServe(req, res);
        }
        
    } catch (Exception e) {
        System.err.println("ERREUR dans handleMultipartRequest:");
        e.printStackTrace();
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur lors du traitement du fichier: " + e.getMessage());
    }
}
    private String readPartValue(Part part) throws IOException {
        try (InputStream inputStream = part.getInputStream();
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
    private boolean isHttpMethodAllowed(URLRoute route, String requestMethod) {
        String routeMethod = route.getHttpMethod().toUpperCase();
        
        System.err.println("=== DEBUG HTTP Method ===");
        System.err.println("Route method: " + routeMethod);
        System.err.println("Request method: " + requestMethod);
        
        // raha tsisy méthode spécifiée (ancien système) na GET, accepter toutes
        if (routeMethod == null || routeMethod.isEmpty() || routeMethod.equals("GET")) {
            System.err.println("  → Ancien système ou GET, autorisé");
            return true;
        }
        
        // Sinon, vérifier la correspondance
        boolean allowed = routeMethod.equals(requestMethod);
        System.err.println("  → Match? " + allowed);
        return allowed;
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

  private void invokeController(URLRoute route, String path, HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
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
        
        Method method = route.getMethod();
        Object controller = route.getController();
        Object result;

       
        if (!SecurityManager.checkAccess(method, controller.getClass(), req, res)) {
            System.out.println("Accès refusé par SecurityManager");
            return; 
        }
        
        Object[] args = ParameterResolver.resolveParameters(method, req, res, urlParams);
        
        System.out.println("Paramètres résolus: " + java.util.Arrays.toString(args));
        
        result = method.invoke(controller, args);

        // Gérer @RemoveSessionAttribute après l'exécution
        RemoveSessionAttribute removeSessionAttr = method.getAnnotation(RemoveSessionAttribute.class);
        if (removeSessionAttr != null) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                for (String attrName : removeSessionAttr.value()) {
                    session.removeAttribute(attrName);
                    System.err.println("Attribut de session supprimé: " + attrName);
                }
            }
        }

        // Traiter le retour selon le type (SEULEMENT si la méthode n'a pas déjà écrit la réponse)
        boolean methodHandledResponse = false;
        for (Object arg : args) {
            if (arg == res) {
                methodHandledResponse = true;
                break;
            }
        }
        
        if (!methodHandledResponse) {
            handleMethodResult(result, req, res, urlParams);
        }

    } catch (Exception e) {
        System.err.println("ERREUR lors de l'invocation du contrôleur: " + e.getMessage());
        e.printStackTrace();

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
    private void handleMethodResult(Object result, HttpServletRequest req, 
                               HttpServletResponse res, Map<String, String> urlParams) 
        throws IOException, ServletException {
    
    if (result instanceof String) {
        // Retour String -> PrintWriter direct
        String responseString = (String) result;
        System.out.println("Retour String: " + responseString);
        res.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = res.getWriter()) {
            out.print(responseString);
        }
        
    } else if (result instanceof ModelView) {
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
    } 
    else if (result instanceof ModelAndView) {
        ModelAndView mv = (ModelAndView) result;
        String viewName = mv.getView();
        System.out.println("Retour ModelAndView -> JSP: " + viewName);
        
        Map<String, Object> model = mv.getModel();
        for (Map.Entry<String, Object> entry : model.entrySet()) {
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
    }
    // Si result est null, ne rien faire
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