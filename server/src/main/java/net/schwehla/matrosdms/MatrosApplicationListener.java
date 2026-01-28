/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class MatrosApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

	org.slf4j.Logger logger = LoggerFactory.getLogger(MatrosApplicationListener.class);

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		logger.info(event.toString());
	}
}
