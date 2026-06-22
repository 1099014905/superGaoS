package com.supergaos.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "MyTestSecretKeyForJWTTokenGeneration2026Test");
        jwtUtil.init();
    }

    @Test
    void generateToken_shouldReturnValidJwt() {
        String token = jwtUtil.generateToken(42L);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");

        // Verify we can parse it back
        SecretKey key = Keys.hmacShaKeyFor(
                "MyTestSecretKeyForJWTTokenGeneration2026Test".getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertEquals("42", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void generateToken_withDifferentUserIds_shouldProduceDifferentTokens() {
        String token1 = jwtUtil.generateToken(1L);
        String token2 = jwtUtil.generateToken(2L);

        assertNotEquals(token1, token2);
    }

    @Test
    void token_shouldExpireAfter24Hours() {
        String token = jwtUtil.generateToken(1L);

        SecretKey key = Keys.hmacShaKeyFor(
                "MyTestSecretKeyForJWTTokenGeneration2026Test".getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        long diffMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(86400000L, diffMs, 1000); // 24h ± 1s
    }
}
