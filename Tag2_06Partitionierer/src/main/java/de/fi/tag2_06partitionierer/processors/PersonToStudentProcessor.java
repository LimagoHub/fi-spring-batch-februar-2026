package de.fi.tag2_06partitionierer.processors;

import de.fi.tag2_06partitionierer.Person;
import de.fi.tag2_06partitionierer.db.entity.StudentEntity;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PersonToStudentProcessor implements ItemProcessor<Person, StudentEntity> {

    @Override
    public StudentEntity process(final Person item) throws Exception {
        return new StudentEntity(UUID.randomUUID(), item.firstName(), item.lastName());
    }
}
