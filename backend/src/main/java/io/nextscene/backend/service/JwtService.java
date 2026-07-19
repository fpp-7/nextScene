package io.nextscene.backend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expirationMs
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email) {
        return JWT.create()
                .withSubject(userId)
                .withClaim("email", email)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusMillis(expirationMs)))
                .withIssuer("nextscene")
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer("nextscene")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public String extractUserId(String token) {
        DecodedJWT decoded = verifyToken(token);
        return decoded != null ? decoded.getSubject() : null;
    }
}
