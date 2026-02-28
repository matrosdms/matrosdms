/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.schwehla.matrosdms.entity.DBItem;

public interface ItemRepository extends JpaRepository<DBItem, Long> {

	@EntityGraph(value = "Item.detail", type = EntityGraph.EntityGraphType.LOAD)
	Optional<DBItem> findByUuid(@Param("uuid") String uuid);

	@Query("SELECT c FROM DBItem c where c.infoContext.id = :id")
	List<DBItem> findAllByContextid(@Param("id") Long pk);

	// Default fetch for Contexts usually wants active only
	@Query("SELECT c FROM DBItem c where c.infoContext.id = :id and c.dateArchived is null order by c.issueDate")
	List<DBItem> findAllNotArchivedByContextid(@Param("id") Long pk);

	// --- MAIN API QUERIES ---

	@Query("SELECT i FROM DBItem i " +
			"LEFT JOIN FETCH i.infoContext c " +
			"LEFT JOIN FETCH i.store s " +
			"LEFT JOIN FETCH i.file f " +
			"ORDER BY c.name ASC, i.id ASC, s.shortname ASC, i.storageItemIdentifier ASC")
	List<DBItem> findAllForReport();

	// 1. ACTIVE ONLY
	@Query("SELECT i FROM DBItem i WHERE "
			+ "i.infoContext.uuid = :contextUuid AND "
			+ "(:query IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND "
			+ "i.dateArchived IS NULL")
	@EntityGraph(attributePaths = { "user", "file" })
	Page<DBItem> findActiveByContextAndQuery(
			@Param("contextUuid") String contextUuid, @Param("query") String query, Pageable pageable);

	// 2. ARCHIVED ONLY (Trash/Recycle Bin)
	@Query("SELECT i FROM DBItem i WHERE "
			+ "i.infoContext.uuid = :contextUuid AND "
			+ "(:query IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND "
			+ "i.dateArchived IS NOT NULL")
	@EntityGraph(attributePaths = { "user", "file" })
	Page<DBItem> findArchivedByContextAndQuery(
			@Param("contextUuid") String contextUuid, @Param("query") String query, Pageable pageable);

	// 3. ALL (Audit/Admin)
	@Query("SELECT i FROM DBItem i WHERE "
			+ "i.infoContext.uuid = :contextUuid AND "
			+ "(:query IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')))")
	Page<DBItem> findAllByContextAndQuery(
			@Param("contextUuid") String contextUuid, @Param("query") String query, Pageable pageable);

	/**
	 * Checks if a file hash exists as either an Original upload
	 * OR as a processed Canonical file.
	 */
	@Query("""
			    SELECT COUNT(m) > 0
			    FROM DBItemMetadata m
			    WHERE m.sha256Original = :hash
			       OR m.sha256Canonical = :hash
			""")
	boolean isDuplicate(@Param("hash") String hash);

	/**
	 * Returns the UUID of the existing item if a file hash already exists
	 * as either an Original upload OR as a processed Canonical file.
	 */
	@Query("""
			    SELECT i.uuid
			    FROM DBItem i
			    JOIN i.file m
			    WHERE m.sha256Original = :hash
			       OR m.sha256Canonical = :hash
			""")
	Optional<String> findDuplicateUuid(@Param("hash") String hash);

	long countByInfoContext_UuidAndDateArchivedIsNull(String uuid);
}