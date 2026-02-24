package de.fi.uebung1;

import de.fi.uebung1.entity.Kuh;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface KuhRepository extends CrudRepository<Kuh, UUID> {
}
