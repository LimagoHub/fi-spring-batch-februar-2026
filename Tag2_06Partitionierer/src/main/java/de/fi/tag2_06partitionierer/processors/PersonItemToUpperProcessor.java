package de.fi.tag2_06partitionierer.processors;


import de.fi.tag2_06partitionierer.Person;
import org.slf4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;


@Component
public class PersonItemToUpperProcessor implements ItemProcessor<Person, Person> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PersonItemToUpperProcessor.class);

    @Override
    public Person process(final Person person) {
        final int id = person.id();
        final String firstName = person.firstName().toUpperCase();
        final String lastName = person.lastName().toUpperCase();
        final Person transformedPerson = new Person(id, firstName, lastName);

        log.info("Converting ({}) into ({})", person, transformedPerson);
        return transformedPerson;
    }
}