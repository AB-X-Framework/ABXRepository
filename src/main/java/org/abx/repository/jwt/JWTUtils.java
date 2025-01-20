package org.abx.repository.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JWTUtils {

    @Value("${jwt.public}")
    private String publicKey;

    public String getPublicKey() {
        return publicKey;
    }
}
