package com.uniquindio.teen_sport;

import com.uniquindio.teen_sport.config.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainersConfiguration.class)
class TeenSportApplicationTests {

	@Test
	void contextLoads() {
	}

}
