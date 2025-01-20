package org.abx.repository.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JWTUtils {

    @Value("${jwt.public}")
    private String publicKey;

    private PublicKey pKey;

    public String getPublicKey() {
        return publicKey;
    }

    private void setup() throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        pKey = keyFactory.generatePublic(keySpec);
    }

    public Claims validateToken(String token) throws Exception {
        if (pKey == null) {
            setup();
        }
        return Jwts.parser()
                .verifyWith(pKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String generateToken(String username,String privateKey) throws Exception {
        long expirationTime = 3600000; // 1 hour in milliseconds
        byte[] decodedKey = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey key = keyFactory.generatePrivate(keySpec);
        return Jwts.builder()
                .issuer(username)  // Subject (e.g., username)
                .issuedAt(new Date()) // Issued at
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) // Expiration
                .signWith(key) // Signing algorithm and key
                .compact();
    }
}
