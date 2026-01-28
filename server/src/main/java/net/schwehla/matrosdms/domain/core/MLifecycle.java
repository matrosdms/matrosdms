/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.core;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record MLifecycle(
    Long id,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime dateCreated,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime dateUpdated,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime dateArchived) {}
