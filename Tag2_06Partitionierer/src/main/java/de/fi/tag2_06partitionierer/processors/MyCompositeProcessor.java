package de.fi.tag2_06partitionierer.processors;

import de.fi.tag2_06partitionierer.Person;
import de.fi.tag2_06partitionierer.db.entity.StudentEntity;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class MyCompositeProcessor extends CompositeItemProcessor<Person, StudentEntity> {

    private final PersonItemToUpperProcessor personItemToUpperProcessor;
    private final PersonToStudentProcessor personToStudentProcessor;

    public MyCompositeProcessor(final PersonItemToUpperProcessor personItemToUpperProcessor, final PersonToStudentProcessor personToStudentProcessor) {
        this.personItemToUpperProcessor = personItemToUpperProcessor;
        this.personToStudentProcessor = personToStudentProcessor;
        super.setDelegates(List.of(personItemToUpperProcessor, personToStudentProcessor));
    }


}
