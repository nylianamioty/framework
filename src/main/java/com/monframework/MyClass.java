package com.monframework;

import com.monframework.annotation.MyAnnotation;

public class MyClass {

    @MyAnnotation(description = "Cette méthode est annotée")
    public void myMethod() {
        System.out.println("Méthode annotée exécutée !");
    }

    public void myOtherMethod() {
        System.out.println("Méthode non annotée exécutée !");
    }

    public static void main(String[] args) {
        MyClass obj = new MyClass();
        obj.myMethod();  // Cette méthode exécutera la logique
        obj.myOtherMethod();
    }
}
