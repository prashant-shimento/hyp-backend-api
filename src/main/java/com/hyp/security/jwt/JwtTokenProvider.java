package com.hyp.security.jwt;

import com.hyp.security.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token.expiration:1800000}") long accessTokenExpiration,
            @Value("${jwt.refresh-token.expiration:2592000000}") long refreshTokenExpiration,
            @Value("${jwt.issuer:hyp-backend-api}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.issuer = issuer;
    }

    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.getUserId())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("userType", principal.getUserType())
                .claim("role", principal.getRole())
                .claim("email", principal.getEmail())
                .claim("mobile", principal.getMobile())
                .claim("name", principal.getName())
                .claim("restaurantId", principal.getRestaurantId())
                .claim("partnerId", principal.getPartnerId())
                .claim("tokenType", "ACCESS")
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.getUserId())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("userType", principal.getUserType())
                .claim("role", principal.getRole())
                .claim("tokenType", "REFRESH")
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate token and extract principal in a single parse. Returns null if the token is
     * invalid, expired, or not an ACCESS token. Replaces the three-call chain
     * validateToken() + isAccessToken() + extractPrincipal() in the auth filter.
     */
    public UserPrincipal validateAndExtractAccessPrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!"ACCESS".equals(claims.get("tokenType"))) {
                return null;
            }

            // isSuperAdmin is intentionally NOT read from the token — the auth filter
            // derives it from the role cache so revocations take effect within one cache TTL.
            // restaurants is intentionally NOT embedded in the token — see generateAccessToken.
            return UserPrincipal.builder()
                    .userId(claims.getSubject())
                    .userType((String) claims.get("userType"))
                    .role((String) claims.get("role"))
                    .email((String) claims.get("email"))
                    .mobile((String) claims.get("mobile"))
                    .name((String) claims.get("name"))
                    .restaurantId((String) claims.get("restaurantId"))
                    .partnerId((String) claims.get("partnerId"))
                    .build();
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Determine the reason why JWT validation failed.
     * Only call this when validateAndExtractAccessPrincipal returns null (token was present but rejected).
     * Returns one of: TOKEN_EXPIRED, INVALID_TOKEN, WRONG_TOKEN_TYPE.
     */
    public String getJwtFailureReason(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            // Token parsed OK — must be wrong type (e.g. REFRESH used as ACCESS)
            return "WRONG_TOKEN_TYPE";
        } catch (ExpiredJwtException e) {
            return "TOKEN_EXPIRED";
        } catch (Exception e) {
            return "INVALID_TOKEN";
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserPrincipal extractPrincipal(String token) {
        try {
            Claims claims = getClaims(token);
            return UserPrincipal.builder()
                    .userId(claims.getSubject())
                    .userType((String) claims.get("userType"))
                    .role((String) claims.get("role"))
                    .email((String) claims.get("email"))
                    .mobile((String) claims.get("mobile"))
                    .name((String) claims.get("name"))
                    .restaurantId((String) claims.get("restaurantId"))
                    .partnerId((String) claims.get("partnerId"))
                    .build();
        } catch (JwtException e) {
            log.error("Error extracting principal from token: {}", e.getMessage());
            return null;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public String getTokenType(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("tokenType");
    }

    public boolean isAccessToken(String token) {
        return "ACCESS".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(getTokenType(token));
    }

    public Date getExpirationDate(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpiration;
    }
}
