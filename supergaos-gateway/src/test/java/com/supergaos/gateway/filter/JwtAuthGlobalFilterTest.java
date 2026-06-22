package com.supergaos.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthGlobalFilterTest {

    private static final String TEST_SECRET = "MyTestSecretKeyForJWTTokenGeneration2026BlogSystem";
    private static final String VALID_TOKEN = createTestToken(1L);

    @InjectMocks
    private JwtAuthGlobalFilter filter;

    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "jwtSecret", TEST_SECRET);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    private static String createTestToken(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }

    // ===== GET whitelist tests =====

    @Test
    void getRequest_toWhitelistedBlogPath_shouldPassThrough() {
        ServerWebExchange exchange = buildGetExchange("/api/blog/articles/1");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void getRequest_toWhitelistedCommentPath_shouldPassThrough() {
        ServerWebExchange exchange = buildGetExchange("/api/comment/articles/5");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void getRequest_toWhitelistedFilePath_shouldPassThrough() {
        ServerWebExchange exchange = buildGetExchange("/api/file/123/download");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void getRequest_toNonWhitelistedPath_shouldRequireAuth() {
        ServerWebExchange exchange = buildGetExchange("/api/user/profile");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Should return 401 because no token
        verify(chain, never()).filter(any());
    }

    // ===== Auth-required whitelist tests =====

    @Test
    void postRequest_toLogin_shouldPassThrough() {
        ServerWebExchange exchange = buildPostExchange("/api/user/login");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void postRequest_toRegister_shouldPassThrough() {
        ServerWebExchange exchange = buildPostExchange("/api/user/register");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    // ===== JWT validation tests =====

    @Test
    void request_withValidToken_shouldPassThroughAndAddHeader() {
        ServerWebExchange exchange = buildExchangeWithToken("/api/user/profile", HttpMethod.GET, VALID_TOKEN);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void request_withMissingToken_shouldReturn401() {
        ServerWebExchange exchange = buildPostExchange("/api/user/profile");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void request_withInvalidToken_shouldReturn401() {
        ServerWebExchange exchange = buildExchangeWithToken("/api/user/profile", HttpMethod.GET, "invalid.jwt.token");

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void request_withExpiredToken_shouldReturn401() throws Exception {
        // Create a token that expired 1 hour ago
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date(System.currentTimeMillis() - 86400000 * 2))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        ServerWebExchange exchange = buildExchangeWithToken("/api/user/profile", HttpMethod.GET, expiredToken);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void request_withoutBearerPrefix_shouldReturn401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/user/profile")
                .header(HttpHeaders.AUTHORIZATION, VALID_TOKEN)  // missing "Bearer "
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ===== Helper methods =====

    private ServerWebExchange buildGetExchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }

    private ServerWebExchange buildPostExchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.post(path).build());
    }

    private ServerWebExchange buildExchangeWithToken(String path, HttpMethod method, String token) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(method, path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        return MockServerWebExchange.from(request);
    }
}
