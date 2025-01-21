package org.abx.repository;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.abx.jwt.JWTUtils;
import org.abx.repository.spring.ABXRepositoryEntry;
import org.abx.services.ServiceRequest;
import org.abx.services.ServiceResponse;
import org.abx.services.ServicesClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.abx.jwt.JWTUtils.Authorities;

@SpringBootTest(classes = ABXRepositoryEntry.class)
public class ABXRepositoryTest {

	@Autowired
	JWTUtils jwtUtils;


	@Autowired
	ServicesClient servicesClient;


	@Value("${jwt.private}")
	private String privateKey;

	@BeforeAll
	public static void setup() {
		SpringApplication.run(ABXRepositoryEntry.class);
	}

	@Test
	public void doBasicTest() throws Exception {
		String username = "dummy";
		List<String> role = List.of("admin");
		String token = JWTUtils.generateToken(username, privateKey, 60,
				role);
		Claims claims = jwtUtils.validateToken(token);
		Assertions.assertEquals(username,
				claims.getSubject());
		Assertions.assertEquals(role,
				claims.get(Authorities));
		token = JWTUtils.generateToken(username, privateKey, 1,
				role);
		Thread.sleep(2000);
		Exception e = null;
		try {
			jwtUtils.validateToken(token);
		} catch (Exception ex) {
			e = ex;
		}
		Assertions.assertNotNull(e);
		Assertions.assertEquals(e.getClass(), ExpiredJwtException.class);
	}

	@Test
	public void reqTest() throws Exception {
		ServiceRequest req = servicesClient.get("repository", "/heartbeat/alive");
		ServiceResponse res = servicesClient.process(req);
		Assertions.assertTrue(res.asBoolean());
		String username = "dummy";
		List<String> role = List.of("admin");
		String token = JWTUtils.generateToken(username, privateKey, 60,
				role);

		req = servicesClient.get("repository", "/heartbeat/user");
		req.jwt(token);
		res = servicesClient.process(req);
		Assertions.assertEquals(username, res.asString());


		req = servicesClient.get("repository", "/heartbeat/admin");
		req.jwt(token);
		res = servicesClient.process(req);
		Assertions.assertTrue(res.asBoolean());

		username = "mini";
		role = List.of("user");
		token = JWTUtils.generateToken(username, privateKey, 60,
				role);
		req = servicesClient.get("repository", "/heartbeat/admin");
		req.jwt(token);
		res = servicesClient.process(req);
		Assertions.assertFalse(res.asBoolean());
	}

}
