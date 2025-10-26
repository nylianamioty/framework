package com.monframework;

import com.monframework.annotation.MyAnnotation;

public class DemoApplication {
    @MyAnnotation(description = "Ceci est une methode annotée")
    public void myMethod() {   
        System.out.println("Méthode exécutée");
    }
    public static void main(String[] args) {
        DemoApplication app = new DemoApplication();
        
        app.myMethod();
    } 
}

