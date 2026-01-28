/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import io.swagger.v3.oas.annotations.media.Schema;

public class CategoryImportMessage {

	@Schema(description = "Category tree structure in YAML format", example = "- Finance:\n    - Invoices\n    - Taxes\n- HR:\n    - Contracts")
	private String yaml;

	@Schema(description = "If true, checks for errors without creating categories", defaultValue = "false")
	private boolean simulate;

	@Schema(description = "If true, deletes existing children before import. If false, fails if Root is not empty.", defaultValue = "false")
	private boolean replace;

	public String getYaml() {
		return yaml;
	}

	public void setYaml(String yaml) {
		this.yaml = yaml;
	}

	public boolean isSimulate() {
		return simulate;
	}

	public void setSimulate(boolean simulate) {
		this.simulate = simulate;
	}

	public boolean isReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}
}
