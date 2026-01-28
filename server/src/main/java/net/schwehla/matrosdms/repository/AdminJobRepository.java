/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.schwehla.matrosdms.entity.admin.DBAdminJob;
import net.schwehla.matrosdms.entity.admin.DBAdminJob.JobStatus;

public interface AdminJobRepository extends JpaRepository<DBAdminJob, Long> {

	boolean existsByStatus(JobStatus status);

	@Query("SELECT j FROM DBAdminJob j WHERE "
			+ "(cast(:from as timestamp) IS NULL OR j.startTime >= :from) AND "
			+ "(cast(:to as timestamp) IS NULL OR j.startTime <= :to)")
	Page<DBAdminJob> findHistory(
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
