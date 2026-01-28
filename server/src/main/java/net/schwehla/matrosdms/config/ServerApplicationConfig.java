/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class ServerApplicationConfig {

	/**
	 * explicit definition to resolve "More than one TaskExecutor" warning. Uses
	 * Java 21 Virtual
	 * Threads for all @Async methods.
	 */
	@Bean(name = "taskExecutor")
	public AsyncTaskExecutor taskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("vt-async-");
		executor.setVirtualThreads(true);
		return executor;
	}
}
