/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.entity.DBGoogleAccount;

public interface GoogleAccountRepository extends JpaRepository<DBGoogleAccount, Long> {

	Optional<DBGoogleAccount> findByUserUuid(String userUuid);

	List<DBGoogleAccount> findByUserUuidIn(Collection<String> userUuids);

	boolean existsByRefreshTokenIsNotNull();

	List<DBGoogleAccount> findByRefreshTokenIsNotNull();

	@Transactional
	@Modifying
	@Query("UPDATE DBGoogleAccount a SET a.lastPulledAt = :at WHERE a.userUuid = :userUuid")
	int updateLastPulledAt(@Param("userUuid") String userUuid, @Param("at") LocalDateTime at);

	// Targeted updates for the sync jobs, so they never clobber a concurrent
	// settings change from the profile page with a stale copy of the row.

	@Transactional
	@Modifying
	@Query("UPDATE DBGoogleAccount a SET a.lastError = :error, a.lastErrorAt = :at WHERE a.userUuid = :userUuid")
	int updateLastError(@Param("userUuid") String userUuid, @Param("error") String error,
			@Param("at") LocalDateTime at);

	@Transactional
	@Modifying
	@Query("UPDATE DBGoogleAccount a SET a.taskListId = :id WHERE a.userUuid = :userUuid")
	int updateTaskListId(@Param("userUuid") String userUuid, @Param("id") String id);

	@Transactional
	@Modifying
	@Query("UPDATE DBGoogleAccount a SET a.calendarId = :id WHERE a.userUuid = :userUuid")
	int updateCalendarId(@Param("userUuid") String userUuid, @Param("id") String id);

	@Transactional
	@Modifying
	@Query("UPDATE DBGoogleAccount a SET a.refreshToken = null, a.lastError = :error, a.lastErrorAt = :at "
			+ "WHERE a.userUuid = :userUuid")
	int markDisconnected(@Param("userUuid") String userUuid, @Param("error") String error,
			@Param("at") LocalDateTime at);
}
