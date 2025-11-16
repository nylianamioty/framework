package com.monframework;

import com.monframework.mapping.ControllerScanner;
import com.monframework.mapping.RouteRegistry;
import com.monframework.mapping.URLRoute;
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

    /**
     * Invoque la méthode du contrôleur correspondant à la route.
     */
   private void invokeController(URLRoute route, String path, HttpServletRequest req, HttpServletResponse res)
        throws IOException {
    try {
        System.out.println("=== DEBUG FrontServlet ===");
        System.out.println("URL demandée: " + path);
        System.out.println("Route trouvée: " + route.getUrlPattern());
        System.out.println("Contrôleur: " + route.getController().getClass().getName());
        System.out.println("Méthode: " + route.getMethod().getName());
        
        Map<String, String> urlParams = route.extractParams(path);
        urlParams.forEach(req::setAttribute);

        Method method = route.getMethod();
        Object controller = route.getController();

        // Invoke la méthode du contrôleur
        Object result = method.invoke(controller, req, res);
        
        // Traiter le retour si c'est une String
        if (result instanceof String) {
            String responseString = (String) result;
            res.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = res.getWriter()) {
                out.print(responseString);
            }
        }

    } catch (Exception e) {
        System.err.println("Erreur lors de l'invocation du contrôleur: " + e.getMessage());
        e.printStackTrace();

        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        res.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = res.getWriter()) {
            out.println("<html><head><title>Erreur Serveur</title></head><body>");
            out.println("<h1>Erreur 500 - Erreur Interne du Serveur</h1>");
            out.println("<p>Une erreur s'est produite lors du traitement de la requête.</p>");
            out.println("<pre>" + e.getMessage() + "</pre>");
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
