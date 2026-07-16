package com.crm.backend;

import com.crm.backend.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
