package org.abx.repository.console;

import org.abx.repository.spring.ABXRepositoryEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ABXRepositoryEntry.class)
class ABXRepositoryTest {

	@Value("${jwt.public}")
	private String publicKey;

	@Test
	public void doBasicTest() {
		System.out.println(publicKey);
	}

}
