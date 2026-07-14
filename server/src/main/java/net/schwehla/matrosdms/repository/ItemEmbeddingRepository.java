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

import net.schwehla.matrosdms.entity.DBItemEmbedding;

public interface ItemEmbeddingRepository extends JpaRepository<DBItemEmbedding, Long> {

	Optional<DBItemEmbedding> findByItemUuid(String itemUuid);

	void deleteByItemUuid(String itemUuid);
}
