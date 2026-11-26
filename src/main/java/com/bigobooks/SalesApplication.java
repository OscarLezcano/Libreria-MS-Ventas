package com.bigobooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservicio de ventas. Se excluye {@link BigoBooksApplication} (la clase
 * de arranque del common) del scan de componentes: esta en el classpath por el
 * JAR compartido y no debe instanciarse dentro de este servicio.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = "com.bigobooks",
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BigoBooksApplication.class))
@EnableJpaRepositories(basePackages = "com.bigobooks.repositories")
@EntityScan(basePackages = { "com.bigobooks.entities", "com.bigobooks.model" })
public class SalesApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SalesApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SalesApplication.class);
	}

}
