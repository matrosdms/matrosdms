/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.ai;

import java.util.List;

public record ClassificationCandidates(List<Candidate> contexts, List<Candidate> categories) {
  public record Candidate(String uuid, String name, String description) {}
}
