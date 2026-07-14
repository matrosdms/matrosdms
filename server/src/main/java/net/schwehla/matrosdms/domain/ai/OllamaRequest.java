/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.ai;

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record OllamaRequest(String model, String prompt, boolean stream, String format) {
  public OllamaRequest(String model, String prompt) {
    this(model, prompt, false, null);
  }

  public OllamaRequest(String model, String prompt, boolean stream) {
    this(model, prompt, stream, null);
  }
}
