package co.za.unicard.security;

import co.za.unicard.config.SecurityConfig;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    public record IssuedToken(String token, Instant expiresAt) {}

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration ttl;

    public JwtService(JwtEncoder encoder, Clock clock, @Value("${unischool.jwt.ttl-minutes:480}") long ttlMinutes) {
        this.encoder = encoder;
        this.clock = clock;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public IssuedToken issue(UserAccount account, RoleAssignment activeRole) {
        Instant now = clock.instant();
        Instant expires = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(account.getUsername())
                .issuedAt(now)
                .expiresAt(expires)
                .claim("uid", account.getId())
                .claim(SecurityConfig.ROLE_CLAIM, activeRole.getRole().name())
                .claim("rid", activeRole.getId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expires);
    }
}
