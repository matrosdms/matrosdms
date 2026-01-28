/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.schwehla.matrosdms.entity.DBStore;

public interface StoreRepository extends JpaRepository<DBStore, Long> {
	Optional<DBStore> findByUuid(String uuid);

	// JPQL to count items belonging to this store
	@Query("SELECT COUNT(i) FROM DBItem i WHERE i.store.uuid = :uuid")
	long countItemsByStoreUuid(@Param("uuid") String uuid);
}
