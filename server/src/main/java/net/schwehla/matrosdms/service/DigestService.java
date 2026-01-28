/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Legacy Service Wrapper. Redirects manual "Digest" requests from the UI to the
 * modern
 * InboxPipelineService.
 */
@Service
public class DigestService {

	private static final Logger LOG = LoggerFactory.getLogger(DigestService.class);

	@Autowired
	InboxPipelineService pipelineService;

	@Async
	public void processInboxItem(String filenameHash) {
		LOG.info("MANUAL TRIGGER: Delegating {} to Pipeline", filenameHash);
		try {
			// FIX: Use new method signature (Just needs Hash now)
			pipelineService.triggerPipeline(filenameHash);
		} catch (Exception e) {
			LOG.error("Failed to trigger pipeline for " + filenameHash, e);
		}
	}
}
