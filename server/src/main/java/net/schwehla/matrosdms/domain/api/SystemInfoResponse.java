/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SystemInfoResponse", description = "System status and version information")
public class SystemInfoResponse {

	@Schema(description = "Application Version", example = "1.0.0")
	private String version;

	@Schema(description = "Application Name", example = "MatrosDMS Server")
	private String name;

	@Schema(description = "System Status", example = "OK")
	private String status;

	@Schema(description = "Current active Tenant based on repository path", example = "Family-Richy-Rich")
	private String tenant;

	public SystemInfoResponse() {
	}

	public SystemInfoResponse(String version, String name, String status, String tenant) {
		this.version = version;
		this.name = name;
		this.status = status;
		this.tenant = tenant;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}
}