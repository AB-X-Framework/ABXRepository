package org.abx.repository.console;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
		String admin = "admin";
		String token =  JWTUtils.generateToken(issuer,privateKey,60,
				admin);
		Claims claims= jwtUtils.validateToken(token);
		Assertions.assertEquals(issuer,
				claims.getIssuer());
		Assertions.assertEquals(admin,
				claims.getSubject());
		 token =  JWTUtils.generateToken(issuer,privateKey,1,
				admin);
		 Thread.sleep(2000);
		 Exception e= null;
		 try {
			 jwtUtils.validateToken(token);
		 }catch (Exception ex){
			 e = ex;
		 }
		 Assertions.assertNotNull(e);
		 Assertions.assertEquals(e.getClass(), ExpiredJwtException.class);
	}

}
