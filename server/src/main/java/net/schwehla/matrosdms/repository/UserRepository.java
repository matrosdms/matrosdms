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

import org.springframework.data.jpa.repository.JpaRepository;

import net.schwehla.matrosdms.domain.core.EUserRole;
import net.schwehla.matrosdms.entity.management.DBUser;

public interface UserRepository extends JpaRepository<DBUser, Long> {

	boolean existsByName(String name);

	List<DBUser> findByRole(EUserRole role);

	Optional<DBUser> findByUuid(String uuid);

	// NEW: Used for login to retrieve the hashed password
	Optional<DBUser> findByName(String name);
}
