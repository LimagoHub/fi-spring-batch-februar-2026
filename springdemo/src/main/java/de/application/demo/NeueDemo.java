package de.application.demo;




import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;



@Component
public class NeueDemo {

    public NeueDemo() {
        System.out.println("Neue Demo Konstruktor");
    }

    @PostConstruct
    public void peter() {
        System.out.println("Peter");
    }
    @PreDestroy
    public void anna() {
        System.out.println("Anna");
    }
}
