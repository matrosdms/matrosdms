/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.schwehla.matrosdms.entity.view.VW_CONTEXT;

@Repository
public interface ContextViewRepository extends JpaRepository<VW_CONTEXT, Long> {
	// The SQL View 'VW_CONTEXT' already filters out archived items and calculates
	// the sum.
	// So findAll() returns the optimized list.
}
