package wardrobe.project.com.storageservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Trích xuất userId (claim "username") từ Authorization header.
     * Header format: "Bearer <token>"
     *
     * @param bearerToken giá trị của Authorization header
     * @return username (userId) trong token
     * @throws IllegalArgumentException nếu token không hợp lệ hoặc thiếu
     */
    public String extractUserId(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        String token = bearerToken.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.get("username", String.class);
            if (username == null) {
                // Fallback: thử lấy subject nếu không có claim "username"
                username = claims.getSubject();
            }
            if (username == null) {
                throw new IllegalArgumentException("Token does not contain username claim");
            }
            return username;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired JWT token");
        }
    }
}
