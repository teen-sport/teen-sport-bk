package com.uniquindio.teen_sport.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Prueba de integración con el contexto completo, PostgreSQL 18.6 en contenedor y MockMvc.
 *
 * <p>Todas las pruebas que la usan comparten la misma configuración y, por tanto, un único
 * contexto y un único contenedor. Las propiedades son solo de prueba: un secreto JWT ficticio,
 * el administrador inicial de {@link TestAccounts} y la creación del esquema en el contenedor. Como la base de datos se comparte entre
 * clases, cada prueba crea sus datos con valores únicos.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(properties = {
        "app.security.jwt.secret=" + TestAccounts.JWT_SECRET,
        "app.bootstrap.admin.email=" + TestAccounts.ADMIN_EMAIL,
        "app.bootstrap.admin.password=" + TestAccounts.ADMIN_PASSWORD,
        "app.bootstrap.admin.id-number=" + TestAccounts.ADMIN_ID_NUMBER,
        // El contenedor arranca vacío: las pruebas sí deben crear el esquema a partir de las entidades.
        "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
public @interface IntegrationTest {
}
