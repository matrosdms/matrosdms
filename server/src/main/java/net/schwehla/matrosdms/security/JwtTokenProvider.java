/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.domain.core.MUser;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

	@Value("${app.security.jwt-secret}")
	private String jwtSecret;

	@Value("${app.security.jwt-expiration-ms}")
	private int jwtExpirationMs;

	private SecretKey getSigningKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateToken(MUser user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

		return Jwts.builder().subject(user.getName()).claim("uuid", user.getUuid()).claim("role", user.getRole())
				.issuedAt(now).expiration(expiryDate).signWith(getSigningKey(), Jwts.SIG.HS256).compact();
	}

	public String getUsernameFromJWT(String token) {
		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload().getSubject();
	}

	public boolean validateToken(String authToken) {
		try {
			Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
			return true;
		} catch (MalformedJwtException ex) {
			logger.error("Invalid JWT token");
		} catch (ExpiredJwtException ex) {
			logger.error("Expired JWT token");
		} catch (UnsupportedJwtException ex) {
			logger.error("Unsupported JWT token");
		} catch (IllegalArgumentException ex) {
			logger.error("JWT claims string is empty.");
		} catch (Exception ex) {
			logger.error("JWT validation error: " + ex.getMessage());
		}
		return false;
	}
}
