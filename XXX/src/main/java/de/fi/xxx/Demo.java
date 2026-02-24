package de.fi.xxx;


import jakarta.annotation.PostConstruct;



public class Demo {


    private Translator translator;


    public Demo(Translator translator) {
        this.translator = translator;
        System.out.println(translator.translate("Demo konstruiert"));
    }

    @PostConstruct
    public void sayHello() {
        System.out.println(translator.translate("Hallo Welt"));
    }
}
