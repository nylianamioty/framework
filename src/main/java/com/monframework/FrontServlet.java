package com.monframework;

import com.monframework.mapping.ControllerScanner;
import com.monframework.mapping.RouteRegistry;
import com.monframework.mapping.URLRoute;
import com.monframework.mvc.ModelView;
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
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
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
        System.out.println("Paramètres Query: " + req.getParameterMap());
        
        Method method = route.getMethod();
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
        // === FIN NOUVELLE PARTIE ===
        
        // Appel de la méthode avec les paramètres résolus
        result = method.invoke(controller, args);

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
