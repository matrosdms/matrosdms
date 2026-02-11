/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.message;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lightweight status update for running jobs")
public class ProgressMessage {

	@Schema(description = "The File ID")
	private String sha256;

    // NEW: Ensure UI can render the card even if it missed the start event
    @Schema(description = "Display name of the file being processed")
    private String filename;

	@Schema(description = "Human readable progress info", example = "OCR Running...")
	private String info;

	@Schema(description = "Current step index (1-based)")
	private int step;

	@Schema(description = "Total number of steps")
	private int totalSteps;

	public ProgressMessage() {
	}

	public ProgressMessage(String sha256, String filename, String info, int step, int totalSteps) {
		this.sha256 = sha256;
        this.filename = filename;
		this.info = info;
		this.step = step;
		this.totalSteps = totalSteps;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getTotalSteps() {
		return totalSteps;
	}

	public void setTotalSteps(int totalSteps) {
		this.totalSteps = totalSteps;
	}
}