package staysplit.hotel_reservation.common.security.jwt;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {

    private final String secretKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;
    private Key encodedSecretKey;
    private final StringRedisTemplate redisTemplate;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.access-expiration-minutes}") int accessTokenExpirationMinutes,
                            @Value("${jwt.refresh-expiration-days}") int refreshTokenExpirationDays,
                            StringRedisTemplate redisTemplate) {
        this.secretKey = secretKey;
        this.accessTokenExpiryMs = accessTokenExpirationMinutes * 60 * 1000L; // 분 -> ms = 분 * 60 seconds * 1000L;
        this.refreshTokenExpiryMs = refreshTokenExpirationDays * 24 * 60 * 60 * 1000L; // 일 = 일 * 24 시간 * 60분 * 60 sec * 1000 ms
        this.encodedSecretKey = new SecretKeySpec(Base64.getDecoder().decode(secretKey),
                SignatureAlgorithm.HS512.getJcaName());
        this.redisTemplate = redisTemplate;
    }

    // access token 생성
    public String createAccessToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        Date now = new Date();
        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpiryMs))
                .signWith(encodedSecretKey)
                .compact();

        return token;
    }

    // refreshToken 생성 & redis에 저장
    public String createRefreshToken(String email) {
        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpiryMs))
                .signWith(encodedSecretKey)
                .compact();

        // Redis에 저장
        String key = "RT:" + refreshToken;

        redisTemplate.opsForValue().set(
                key,                        // key
                email,                      // value
                refreshTokenExpiryMs,       // timeout
                TimeUnit.MILLISECONDS       // TTL 단위
        );
        return refreshToken;
    }

    // access token 연장
    // validate refresh token
    public TokenPair recreateAccessTokenAndRotateRefreshToken(String refreshToken) {

        // REDIS에 refresh token이 존재하는지 확인
        String key = "RT:" + refreshToken;
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new IllegalArgumentException("Refresh Token이 유효하지 않습니다.");
        }

        try {
            // Refresh Token이 DB에 있는 경우, JWT 서명 검증 + 만료 검증
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(refreshToken);

            Claims claims = claimsJws.getBody();

            if (claims.getExpiration().before(new Date())) {
                throw new IllegalArgumentException("Refresh Token이 만료되었습니다. 다시 로그인 해주세요.");
            }

            // 회전
            redisTemplate.delete(refreshToken);

            // 새 refresh token & access token 발급
            String accessToken = createAccessToken(email);
            String newRefreshToken = createRefreshToken(email);
            return new TokenPair(accessToken, newRefreshToken);

        } catch (JwtException | IllegalArgumentException exception) {
            // 파싱 실패, 서명 불일치 -> Redis에서 삭제
            redisTemplate.delete(refreshToken);
            throw new IllegalArgumentException("Refresh Token이 유효하지 않습니다.", exception);
        }
    }

    // 로그아웃 시 refresh token 무효화
    public void invalidateRefreshToken(String refreshToken) {
        redisTemplate.delete("RT:" + refreshToken);
    }

    public record TokenPair(
        String accessToken,
        String refreshToken)
    {}
}

