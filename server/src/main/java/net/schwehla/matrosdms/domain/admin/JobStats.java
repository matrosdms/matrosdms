/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.domain.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Background Processing Statistics")
public class JobStats {

	@Schema(description = "Jobs successfully finished")
	private long succeeded;

	@Schema(description = "Jobs currently running")
	private long processing;

	@Schema(description = "Jobs failed (requiring attention)")
	private long failed;

	@Schema(description = "Jobs waiting in queue")
	private long enqueued;

	@Schema(description = "URL to the JobRunr Dashboard")
	private String dashboardUrl;

	// Constructor
	public JobStats(
			long succeeded, long processing, long failed, long enqueued, String dashboardUrl) {
		this.succeeded = succeeded;
		this.processing = processing;
		this.failed = failed;
		this.enqueued = enqueued;
		this.dashboardUrl = dashboardUrl;
	}

	// Getters
	public long getSucceeded() {
		return succeeded;
	}

	public long getProcessing() {
		return processing;
	}

	public long getFailed() {
		return failed;
	}

	public long getEnqueued() {
		return enqueued;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}
}
