package com.example.photoprintapplication1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;  // ← ДОБАВИТЬ ЭТУ СТРОКУ

@SpringBootTest(classes = PhotoprintApplication.class)
@ActiveProfiles("ci")
class PhotoPrintApplicationTests {

	@Test
	void contextLoads() {
	}

}