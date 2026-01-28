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
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.schwehla.matrosdms.entity.management.DBConfig;

@Repository
public interface ConfigRepository extends JpaRepository<DBConfig, Long> {

	// Maps to the @NamedQuery "DBConfig.findByKey" defined in DBConfig entity
	Optional<DBConfig> findByKey(@Param("key") String key);
}
