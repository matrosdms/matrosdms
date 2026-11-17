/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.domain.action.ESyncTarget;
import net.schwehla.matrosdms.entity.DBActionSync;

public interface ActionSyncRepository extends JpaRepository<DBActionSync, Long> {

	List<DBActionSync> findByActionUuid(String actionUuid);

	boolean existsByActionUuid(String actionUuid);

	List<DBActionSync> findByAccountUserUuidAndTarget(String accountUserUuid, ESyncTarget target);

	@Query("SELECT DISTINCT s.actionUuid FROM DBActionSync s WHERE s.accountUserUuid = :userUuid")
	List<String> findActionUuidsByAccount(@Param("userUuid") String userUuid);

	long countByAccountUserUuidAndTarget(String accountUserUuid, ESyncTarget target);

	@Transactional
	@Modifying
	@Query("DELETE FROM DBActionSync s WHERE s.accountUserUuid = :userUuid")
	int deleteByAccount(@Param("userUuid") String userUuid);
}
