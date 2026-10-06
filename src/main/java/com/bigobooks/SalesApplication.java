package com.bigobooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Microservicio de ventas. Se excluye {@link BigoBooksApplication} (la clase
 * de arranque del common) del scan de componentes: esta en el classpath por el
 * JAR compartido y no debe instanciarse dentro de este servicio.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.bigobooks",
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BigoBooksApplication.class))
@EnableJpaRepositories(basePackages = "com.bigobooks.repositories")
@EntityScan(basePackages = { "com.bigobooks.entities", "com.bigobooks.model" })
public class SalesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalesApplication.class, args);
	}

}
