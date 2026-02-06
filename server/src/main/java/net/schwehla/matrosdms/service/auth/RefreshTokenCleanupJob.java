/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service.auth;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import net.schwehla.matrosdms.repository.RefreshTokenRepository;

@Component
public class RefreshTokenCleanupJob {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
	private final RefreshTokenRepository refreshTokenRepository;

	public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
		this.refreshTokenRepository = refreshTokenRepository;
	}

	@Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
	@Transactional
	public void cleanupExpiredTokens() {
		int deleted = refreshTokenRepository.deleteExpiredTokens(Instant.now());
		if (deleted > 0) {
			log.info("🔐 Security Cleanup: Removed {} expired refresh tokens.", deleted);
		}
	}
}