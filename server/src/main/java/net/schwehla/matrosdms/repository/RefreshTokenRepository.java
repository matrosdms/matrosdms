/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import net.schwehla.matrosdms.entity.management.DBRefreshToken;
import net.schwehla.matrosdms.entity.management.DBUser;

@Repository
public interface RefreshTokenRepository extends JpaRepository<DBRefreshToken, Long> {

	Optional<DBRefreshToken> findByToken(String token);

	@Modifying
	int deleteByUser(DBUser user);

	@Modifying
	@Query("DELETE FROM DBRefreshToken t WHERE t.expiryDate < :now")
	int deleteExpiredTokens(Instant now);
}