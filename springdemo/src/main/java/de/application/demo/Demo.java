package de.application.demo;

import de.application.translator.Translator;

public class Demo {

    private String message="Hallo Welt";
    private Translator translator;

//    public Demo(final Translator translator) {
//        this.translator = translator;
//    }

    public Demo() {

        System.out.println(message);
    }

    public Translator getTranslator() {
        return translator;
    }

    public void setTranslator(final Translator translator) {
        this.translator = translator;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
        System.out.println("Setter called");
    }

    private void init() {
        System.out.println(translator.translate("Hier ist init"));
    }

    public void foo() {
        System.out.println(translator.translate(getMessage()));
    }

    public void dispose() {
        System.out.println("und tschuess...");
    }
}
