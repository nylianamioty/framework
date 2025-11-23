package com.monframework;

import com.monframework.mapping.ControllerScanner;
import com.monframework.mapping.RouteRegistry;
import com.monframework.mapping.URLRoute;
import com.monframework.mvc.ModelView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * FrontServlet : point d'entrée de toutes les requêtes HTTP.
 * - Sert les ressources statiques via le dispatcher par défaut.
 * - Route les URLs dynamiques vers les contrôleurs.
 */
public class FrontServlet extends HttpServlet {

    private RequestDispatcher defaultDispatcher;
    private RouteRegistry routeRegistry;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        routeRegistry = new RouteRegistry();

        // Récupère le package des contrôleurs depuis le paramètre d'init
        String basePackage = config.getInitParameter("controller-package");
        if (basePackage == null || basePackage.isEmpty()) {
            basePackage = "com.giga.springlab.controller";
        }

        System.out.println("=== Initialisation du FrontServlet ===");
        System.out.println("Scan du package: " + basePackage);

        // Scan et enregistre les routes des contrôleurs
        List<URLRoute> routes = ControllerScanner.scanPackage(basePackage);
        routeRegistry.registerRoutes(routes);

        // Affiche les routes enregistrées
        routeRegistry.printRoutes();
        System.out.println("=== FrontServlet initialisé ===");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            // Sert les fichiers statiques (JSP, HTML, etc.)
            defaultServe(req, res);
        } else {
            // Route dynamique
            URLRoute route = routeRegistry.findRoute(path);
            if (route != null) {
                invokeController(route, path, req, res);
            } else {
                customServe(req, res);
            }
        }
    }

   
private void invokeController(URLRoute route, String path, HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
    try {
        System.out.println("=== DEBUG FrontServlet ===");
        System.out.println("URL demandée: " + path);
        System.out.println("Route trouvée: " + route.getUrlPattern());
        System.out.println("Contrôleur: " + route.getController().getClass().getName());
        System.out.println("Méthode: " + route.getMethod().getName());
        
        Map<String, String> urlParams = route.extractParams(path);
        System.out.println("Paramètres URL extraits: " + urlParams);
        
        Method method = route.getMethod();
        Object controller = route.getController();
        Object result;

        Class<?>[] paramTypes = method.getParameterTypes();
        System.out.println("Paramètres méthode: " + paramTypes.length);
        
        if (paramTypes.length == 0) {
            System.out.println("Appel sans paramètres");
            result = method.invoke(controller);
            
        } else if (paramTypes.length == 1 && paramTypes[0] == String.class && urlParams.size() == 1) {
            String paramValue = urlParams.values().iterator().next();
            System.out.println("Appel avec 1 paramètre String: " + paramValue);
            result = method.invoke(controller, paramValue);
            
        } else if (paramTypes.length == urlParams.size()) {
            System.out.println("Appel avec " + paramTypes.length + " paramètres");
            Object[] args = new Object[paramTypes.length];
            String[] paramNames = urlParams.keySet().toArray(new String[0]);
            
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] == String.class) {
                    args[i] = urlParams.get(paramNames[i]);
                    System.out.println("Paramètre " + i + " (" + paramNames[i] + "): " + args[i]);
                } else {
                    throw new RuntimeException("Type de paramètre non supporté: " + paramTypes[i]);
                }
            }
            result = method.invoke(controller, args);
            
        } else if (paramTypes.length == 2 && 
                   paramTypes[0] == HttpServletRequest.class && 
                   paramTypes[1] == HttpServletResponse.class) {
            System.out.println("Appel avec (req, res)");
            urlParams.forEach(req::setAttribute);
            result = method.invoke(controller, req, res);
            
        } else {
            String errorMsg = "Signature non supportée: " + method.getName() + 
                            " - Paramètres attendus: " + paramTypes.length + 
                            " - Paramètres URL: " + urlParams.size();
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (result instanceof String) {
            // Cas 1: Retour String -> PrintWriter direct
            String responseString = (String) result;
            System.out.println("Retour String: " + responseString);
            res.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = res.getWriter()) {
                out.print(responseString);
            }
            
        } else if (result instanceof ModelView) {
            // Cas 2: Retour ModelView -> Forward vers JSP
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
            
        } else if (result == null) {
            // Retour void
            System.out.println("Méthode sans retour (void)");
        } else {
            System.out.println("Type de retour non géré: " + result.getClass().getName());
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
    /**
     * Affiche une page d'erreur 404 personnalisée.
     */
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

    /**
     * Sert les fichiers statiques via le dispatcher par défaut.
     */
    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}
