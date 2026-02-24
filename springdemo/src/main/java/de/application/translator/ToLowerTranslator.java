package de.application.translator;

public class ToLowerTranslator implements Translator{
    @Override
    public String translate(final String messageToTranslate) {
        return messageToTranslate.toLowerCase();
    }
}
