package de.application;


import de.application.client.CalcClient;
import de.application.demo.Demo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@ComponentScan(basePackages = "de")
public class Main {



	public static void main(String[] args) {


		final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Main.class);

		context.registerShutdownHook();

		//context.getBean(CalcClient.class).go();




	}

}
