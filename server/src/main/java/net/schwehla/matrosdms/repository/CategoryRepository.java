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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.schwehla.matrosdms.entity.DBCategory;

public interface CategoryRepository extends JpaRepository<DBCategory, Long> {

	Optional<DBCategory> findByUuid(String uuid);

	@Query("SELECT c FROM DBCategory c WHERE c.parent.id = :parentId AND c.name = :name")
	Optional<DBCategory> findChildByName(
			@Param("parentId") Long parentId, @Param("name") String name);

	List<DBCategory> findByParent(DBCategory parent);

	long countByParent(DBCategory parent);

	// NEW: For Autocomplete / Suggestions
	// Finds categories where name matches query AND falls under a specific Root
	// (e.g. ROOT_WHERE)
	// We use a native recursive query or simple path check.
	// For simplicity and speed in suggestion, we check if the category belongs to
	// the tree.
	// Note: This query assumes direct parent for simplicity, or we can just search
	// by name globaly if
	// unique.
	// Better: Search all categories where name like X. Frontend filters context.
	@Query("SELECT c.name FROM DBCategory c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
	List<String> suggestNames(@Param("query") String query, Pageable pageable);

	// More precise: Suggest only children of specific Root UUID (Recursive via code
	// or join)
	// Here we do a join on parent to find root-level, but for deep trees, standard
	// Hibernate is
	// tricky without CTE.
	// We will stick to global name suggestion for now, or filter in Service.
}
