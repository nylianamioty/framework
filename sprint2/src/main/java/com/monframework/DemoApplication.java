package com.monframework;

import com.monframework.annotation.MyAnnotation;
import com.monframework.annotation.MyClassAnnotation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MyClassAnnotation(
    value = "Classe principale de démonstration",
    author = "classs",
    version = "2.0"
)
@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DÉMARRAGE DU SCANNING ===");
        
        // Exécuter les méthodes
        myMethod();
        anotherMethod();
        regularMethod();
        
        // Lancer le scanning
        System.out.println("\n=== RÉSULTATS DU SCANNING ===");
        com.monframework.scanner.AnnotationScanner.main(new String[]{});
        
        System.out.println("=== FIN ===");
    }
    
    @MyAnnotation(description = "Ceci est une méthode annotée")
    public void myMethod() {   
        System.out.println("Méthode annotée 1 exécutée");
    }
    
    @MyAnnotation(description = "Une autre méthode de test")
    public void anotherMethod() {
        System.out.println("Méthode annotée 2 exécutée");
    }
    
    // Méthode NON annotée (ne sera pas détectée par le scanner)
    public void regularMethod() {
        System.out.println("Méthode régulière (non annotée) exécutée");
    }
}