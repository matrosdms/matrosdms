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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.schwehla.matrosdms.entity.DBContext;

public interface ContextRepository extends JpaRepository<DBContext, Long> {

	@EntityGraph(attributePaths = { "categoryList" })
	Optional<DBContext> findByUuid(String uuid);

	@Query("SELECT c.name FROM DBContext c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) AND"
			+ " c.dateArchived IS NULL")
	List<String> suggestNames(@Param("query") String query, Pageable pageable);
}
