package de.fi.first.batchprocessing;


import de.fi.first.entity.Person;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;




public class PersonItemProcessor implements ItemProcessor<Person, Person> {

    Logger log = LoggerFactory.getLogger(PersonItemProcessor.class);

    @Override
    public Person process(final Person person) {
        final String firstName = person.getFirstName().toUpperCase();
        final String lastName = person.getLastName().toUpperCase();
        final Person transformedPerson = new Person(firstName, lastName, 10);

        log.info("Converting ({}) into ({})", person, transformedPerson);
        return transformedPerson; // Was passiert wenn der Processor null zurück gibt? -> Element wird raus gefiltert
    }
}