/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import java.time.Instant;

import net.schwehla.matrosdms.domain.admin.EJobStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details of a background scheduled task")
public record JobMessage(
    @Schema(description = "The type of job", example = "ingest-file") String taskName,
    @Schema(
            description = "Unique identifier for this specific execution",
            example = "ingest-12345-abc")
        String instanceId,
    @Schema(description = "When the job is scheduled to run (or started running)", nullable = true)
        Instant executionTime,
    @Schema(description = "Current lifecycle state") EJobStatus status) {}
