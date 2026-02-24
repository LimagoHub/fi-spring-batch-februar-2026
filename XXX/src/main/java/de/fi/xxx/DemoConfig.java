package de.fi.xxx;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoConfig {




    @Bean
    public Demo createDemo(Translator translator){
        return new Demo(translator);
    }
}
