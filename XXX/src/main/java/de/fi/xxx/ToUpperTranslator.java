package de.fi.xxx;


import org.springframework.stereotype.Component;

@Component
public class ToUpperTranslator implements Translator{
    @Override
    public String translate(final String text) {
        return text.toUpperCase();
    }
}
