package org.abx.repository.console;

import org.abx.repository.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ABXRepositoryEntry.class)
class ABXRepositoryTest {

	@Autowired
	JWTUtils jwtUtils;

	@Value("${jwt.private}")
	private String privateKey;

	@Test
	public void doBasicTest()throws Exception{
		String issuer = "dummy";
		String token =  JWTUtils.generateToken(issuer,privateKey);
		Assertions.assertEquals(issuer,
				jwtUtils.validateToken(token).getIssuer());
	}

}
