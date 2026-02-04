/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.schwehla.matrosdms.domain.action.EActionStatus;
import net.schwehla.matrosdms.entity.DBAction;

public interface ActionRepository extends JpaRepository<DBAction, Long> {

	Optional<DBAction> findByUuid(String uuid);

	@Query("SELECT a FROM DBAction a WHERE "
			+ "(:statuses IS NULL OR a.status IN :statuses) AND "
			+ "(:assigneeUuid IS NULL OR a.assignee.uuid = :assigneeUuid) AND "
			+ "(cast(:minDate as timestamp) IS NULL OR a.dueDate >= :minDate)")
	Page<DBAction> findByFilters(
			@Param("statuses") List<EActionStatus> statuses,
			@Param("assigneeUuid") String assigneeUuid,
			@Param("minDate") LocalDateTime minDate,
			Pageable pageable);

	List<DBAction> findByItemUuid(String itemUuid);
	
    @Query("SELECT a FROM DBAction a " +
           "JOIN FETCH a.item " + 
           "JOIN FETCH a.assignee " + 
           "WHERE a.status NOT IN ('DONE', 'REJECTED') order by a.uuid")
    List<DBAction> findAllOpenActions();
    
}
