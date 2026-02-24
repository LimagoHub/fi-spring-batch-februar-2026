package de.application.translator;

public class ToUpperTranslator implements Translator{
    @Override
    public String translate(final String messageToTranslate) {
        return messageToTranslate.toUpperCase();
    }
}
