package com.bigobooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Carga del contexto completo. Requiere que exista
 * src/main/resources/config/private.properties con una base de datos accesible;
 * se ejecuta solo si se define la propiedad de sistema sales.test.full=true.
 */
@SpringBootTest(classes = SalesApplication.class)
@EnabledIfSystemProperty(named = "sales.test.full", matches = "true")
class SalesApplicationTests {

	@Test
	void contextLoads() {
	}

}
