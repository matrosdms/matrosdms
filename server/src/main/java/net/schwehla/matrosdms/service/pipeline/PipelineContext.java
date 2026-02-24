/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.pipeline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;

import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.service.PdfConversionService.AnalysisResult;
import net.schwehla.matrosdms.service.message.DigestResultMessage;
import net.schwehla.matrosdms.service.pipeline.PipelineEvents.PipelineProgressEvent;

public class PipelineContext {
	private final String hash;
	private final Path workingDir;
	private final Path originalFile;
	private final ApplicationEventPublisher publisher;
	private final int totalSteps;

	private int currentStepIndex = 0;
	private InboxFile currentState;

	// --- State Cache ---
	private Path processedFile;
	private String extractedText;
	private DigestResultMessage aiResult;
	private List<String> warnings = new ArrayList<>();

	// NEW: Optimization Flags
	private String cachedMimeType;
	private AnalysisResult pdfAnalysis; // Caches PDF text layer info

	public PipelineContext(
			String hash,
			Path workingDir,
			Path originalFile,
			String originalFilename,
			ApplicationEventPublisher publisher,
			int totalSteps) {
		this.hash = hash;
		this.workingDir = workingDir;
		this.originalFile = originalFile;
		this.publisher = publisher;
		this.totalSteps = totalSteps;

		// Init State
		this.currentState = new InboxFile();
		this.currentState.setSha256(hash);
		this.currentState.getFileInfo().setOriginalFilename(originalFilename);

		this.aiResult = new DigestResultMessage();
		// Default to original unless processed
		this.processedFile = originalFile;
	}

	public void log(String message) {
		if (publisher != null) {
			publisher.publishEvent(
					new PipelineProgressEvent(hash, getDisplayFilename(), message, currentStepIndex, totalSteps));
		}
	}

	public void addWarning(String warning) {
		this.warnings.add(warning);
	}

	// --- Getters / Setters ---

	public InboxFile getCurrentState() {
		return currentState;
	}

	// Optimized MIME handling
	public void setMimeType(String mime) {
		this.cachedMimeType = mime;
		currentState.getFileInfo().setContentType(mime);
	}

	public String getMimeType() {
		return cachedMimeType; // Returns cached value
	}

	public void setExtension(String ext) {
		currentState.getFileInfo().setExtension(ext);
	}

	public String getExtension() {
		return currentState.getFileInfo().getExtension();
	}

	public String getDisplayFilename() {
		return currentState.getDisplayName();
	}

	public String getHash() {
		return hash;
	}

	public Path getWorkingDir() {
		return workingDir;
	}

	public Path getOriginalFile() {
		return originalFile;
	}

	public Path getProcessedFile() {
		return processedFile;
	}

	public void setProcessedFile(Path processedFile) {
		this.processedFile = processedFile;
	}

	public String getExtractedText() {
		return extractedText;
	}

	public void setExtractedText(String extractedText) {
		this.extractedText = extractedText;
	}

	public DigestResultMessage getAiResult() {
		return aiResult;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public ApplicationEventPublisher getPublisher() {
		return publisher;
	}

	public void setCurrentStepIndex(int i) {
		this.currentStepIndex = i;
	}

	// PDF Optimization
	public AnalysisResult getPdfAnalysis() {
		return pdfAnalysis;
	}

	public void setPdfAnalysis(AnalysisResult pdfAnalysis) {
		this.pdfAnalysis = pdfAnalysis;
	}
}