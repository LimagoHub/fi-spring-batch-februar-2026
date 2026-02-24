package de.fi.tag2_06partitionierer.db;

import de.fi.tag2_06partitionierer.db.entity.StudentEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface StudentRepository extends CrudRepository<StudentEntity, UUID> {
}
