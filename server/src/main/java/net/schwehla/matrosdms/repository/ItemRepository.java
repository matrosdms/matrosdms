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

	// Optimized Fetch: Get Item + User + Context + Metadata in 1 Query
	@EntityGraph(value = "Item.detail", type = EntityGraph.EntityGraphType.LOAD)
	Optional<DBItem> findByUuid(@Param("uuid") String uuid);

	@Query("SELECT c FROM DBItem c where c.infoContext.id = :id")
	List<DBItem> findAllByContextid(@Param("id") Long pk);

	@Query("SELECT c FROM DBItem c where c.infoContext.id = :id and c.dateArchived is null order by"
			+ " c.issueDate")
	List<DBItem> findAllNotArchivedByContextid(@Param("id") Long pk);

	// Note: Search results (Page<DBItem>) usually come from Hibernate Search
	// (SearchService),
	// which returns the ID list. When Hydrating entities, we rely on batch fetching
	// defined in
	// application.yaml
	// (hibernate.default_batch_fetch_size = 50) to solve N+1 for lists efficiently
	// without complex
	// graphs here.

	@Query("SELECT i FROM DBItem i WHERE "
			+ "i.infoContext.uuid = :contextUuid AND "
			+ "(:query IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND "
			+ "i.dateArchived IS NULL")
	@EntityGraph(attributePaths = { "user", "file" }) // Quick ad-hoc graph for lists
	Page<DBItem> findActiveByContextAndQuery(
			@Param("contextUuid") String contextUuid, @Param("query") String query, Pageable pageable);

	@Query("SELECT i FROM DBItem i WHERE "
			+ "i.infoContext.uuid = :contextUuid AND "
			+ "(:query IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')))")
	Page<DBItem> findAllByContextAndQuery(
			@Param("contextUuid") String contextUuid, @Param("query") String query, Pageable pageable);

	boolean existsByFileSha256Original(String sha256);

	long countByInfoContext_UuidAndDateArchivedIsNull(String uuid);
}
