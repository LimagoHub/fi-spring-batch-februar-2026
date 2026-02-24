package de.fi.uebung1;


import de.fi.uebung1.entity.Kuh;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class Demo {

    private final KuhRepository kuhRepository;

    public Demo(final KuhRepository kuhRepository) {
        this.kuhRepository = kuhRepository;
    }

    @PostConstruct
    public void saveuh() {
        Kuh elsa = new Kuh(UUID.randomUUID(), "Elsa",10);
        kuhRepository.save(elsa);
    }
}
