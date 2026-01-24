package com.monframework.security;

import com.monframework.annotation.Authenticated;
import com.monframework.annotation.RoleAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Method;

public class SecurityManager {
    
    public static boolean checkAccess(Method method, Class<?> controllerClass, 
                                     HttpServletRequest request, HttpServletResponse response) 
                                     throws IOException {
        
        String path = request.getRequestURI().substring(request.getContextPath().length());
        System.err.println("=== SECURITY MANAGER - Vérification: " + path + " ===");
        
        // Étape 1: Récupérer les annotations
        Authenticated methodAuth = method.getAnnotation(Authenticated.class);
        Authenticated classAuth = controllerClass.getAnnotation(Authenticated.class);
        
        RoleAllowed methodRole = method.getAnnotation(RoleAllowed.class);
        RoleAllowed classRole = controllerClass.getAnnotation(RoleAllowed.class);
        
        // Étape 2: Déterminer quelle annotation utiliser (méthode prioritaire sur classe)
        Authenticated authToUse = methodAuth != null ? methodAuth : classAuth;
        RoleAllowed roleToUse = methodRole != null ? methodRole : classRole;
        
        // Étape 3: Vérifier l'authentification si nécessaire
        if (authToUse != null) {
            System.err.println("→ Authentification requise");
            if (!isAuthenticated(request, authToUse.sessionAttribute())) {
                System.err.println("→ NON AUTHENTIFIÉ - Redirection vers: " + authToUse.redirect());
                response.sendRedirect(request.getContextPath() + authToUse.redirect());
                return false;
            }
            System.err.println("→ Authentifié ✓");
        }
        
        // Étape 4: Vérifier le rôle si nécessaire
        if (roleToUse != null) {
            System.err.println("→ Rôle(s) requis: " + String.join(", ", roleToUse.value()));
            
            // Pour vérifier un rôle, il faut déjà être authentifié
            if (!isAuthenticated(request, "user")) {
                System.err.println("→ Non authentifié pour vérification de rôle");
                String redirect = roleToUse.redirect().isEmpty() ? "/login" : roleToUse.redirect();
                response.sendRedirect(request.getContextPath() + redirect);
                return false;
            }
            
            if (!hasRequiredRole(request, roleToUse.value(), roleToUse.roleAttribute())) {
                System.err.println("→ Rôle INSUFFISANT - Redirection vers: " + roleToUse.redirect());
                response.sendRedirect(request.getContextPath() + roleToUse.redirect());
                return false;
            }
            System.err.println("→ Rôle OK ✓");
        }
        
        System.err.println("=== ACCÈS AUTORISÉ ===");
        return true;
    }
    
    /**
     * Vérifie si l'utilisateur est authentifié
     */
    private static boolean isAuthenticated(HttpServletRequest request, String sessionAttribute) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.err.println("    → Aucune session active");
            return false;
        }
        
        Object authValue = session.getAttribute(sessionAttribute);
        boolean authenticated = (authValue != null);
        
        System.err.println("    → Vérification attribut '" + sessionAttribute + "': " + 
                          (authValue != null ? authValue.toString() : "null"));
        
        return authenticated;
    }
    
    /**
     * Vérifie si l'utilisateur a l'un des rôles requis
     */
    private static boolean hasRequiredRole(HttpServletRequest request, String[] requiredRoles, 
                                         String roleAttribute) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        Object roleObj = session.getAttribute(roleAttribute);
        if (roleObj == null) {
            System.err.println("    → Aucun rôle trouvé dans l'attribut: " + roleAttribute);
            return false;
        }
        
        String userRole = roleObj.toString();
        System.err.println("    → Rôle utilisateur: " + userRole);
        
        for (String requiredRole : requiredRoles) {
            if (requiredRole.equalsIgnoreCase(userRole)) {
                return true;
            }
        }
        
        System.err.println("    → Rôle non autorisé. Requis: " + String.join(", ", requiredRoles));
        return false;
    }
    
    /**
     * Méthode utilitaire pour authentifier un utilisateur
     */
    public static void authenticateUser(HttpServletRequest request, String username, String role) {
        HttpSession session = request.getSession(true);
        session.setAttribute("user", username);
        session.setAttribute("role", role);
        System.err.println("Utilisateur authentifié: " + username + " (rôle: " + role + ")");
    }
    
    /**
     * Méthode utilitaire pour déconnecter un utilisateur
     */
    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            System.err.println("Session invalidée");
        }
    }
}