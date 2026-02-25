/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.bootstrap;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.search.SearchCriteria;
import net.schwehla.matrosdms.service.InboxWatchService;
import net.schwehla.matrosdms.service.SearchService;
import net.schwehla.matrosdms.service.auth.RefreshTokenCleanupJob;
import net.schwehla.matrosdms.service.domain.ActionService;
import net.schwehla.matrosdms.service.domain.AttributeLookupService;
import net.schwehla.matrosdms.service.domain.AttributeService;
import net.schwehla.matrosdms.service.domain.ContextService;
import net.schwehla.matrosdms.service.domain.StoreService;
import net.schwehla.matrosdms.service.domain.UserService;
import net.schwehla.matrosdms.service.mail.imap.ImapServer;
import net.schwehla.matrosdms.service.mail.smtp.SmtpServer;

@Component
public class SpringBootAfterStarter implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(SpringBootAfterStarter.class);

	private int serverPort;

	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired private WebServerApplicationContext 
	webServerAppCtx;
	
	@Value("${app.start-browser:false}")
	private boolean startBrowser;


	@Autowired
	AttributeLookupService attributeLookupService;
	@Autowired
	SearchService searchService;
	@Autowired
	ContextService contextService;
	@Autowired
	StoreService storeService;
	@Autowired
	UserService userService;
	@Autowired
	AttributeService attributeService;
	@Autowired
	ActionService actionService;

	// NEW: Inject Cleanup Job
	@Autowired
	RefreshTokenCleanupJob tokenCleanupJob;

	// FIX: Autowiring these forces Spring to instantiate them despite
	// 'lazy-initialization: true'
	// This triggers their @PostConstruct start() methods.
	@Autowired
	SmtpServer smtpServer;
	@Autowired
	ImapServer imapServer;

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		

		this.serverPort = webServerAppCtx.getWebServer().getPort();
		
		log.info("HTTP Server is READY on " + serverPort + " -  Starting Background Services...");

		// 1. Start File Watcher
		attributeLookupService.refresh();
		InboxWatchService watchService = applicationContext.getBean(InboxWatchService.class);
		Thread.ofVirtual().name("inbox-watcher").start(watchService);

		// 2. Confirm Mail Servers
		log.info(
				"Mail Services Active: SMTP (Port {}) & IMAP (Port {})",
				2525,
				1143);

		// 3. Data Warm-up & Housekeeping
		CompletableFuture.runAsync(this::warmUpSystem);

		// NEW: Cleanup tokens immediately (for desktop/dev usage patterns)
		CompletableFuture.runAsync(() -> {
			try {
				tokenCleanupJob.cleanupExpiredTokens();
			} catch (Exception e) {
				log.warn("Startup token cleanup skipped: {}", e.getMessage());
			}
		});

		log.info("Background tasks scheduled.");
		
		// 4. Auto-open Browser (development convenience)
		if (startBrowser) { 
			CompletableFuture.runAsync(this::openBrowser); 
		}
		

	}

	private void warmUpSystem() {
		long start = System.currentTimeMillis();
		try {
			searchService.search(SearchCriteria.forText("invoice"), 0, 1);
			contextService.loadContextList(EArchiveFilter.ACTIVE_ONLY, 100, "name");
			storeService.loadStoreList();
			userService.loadUserList();
			attributeService.loadAttributeTypes();
			actionService.searchActions(null, null, null, PageRequest.of(0, 20, Sort.by("dueDate")));

			log.info("System Warmed Up in {}ms", (System.currentTimeMillis() - start));
		} catch (Exception e) {
			log.debug("Warm-up optimization skipped: {}", e.getMessage());
		}
	}

	private void openBrowser() {
		try {

			log.info("app is running in: http://localhost:" + serverPort);

			log.info("Start Browser");
			System.setProperty("java.awt.headless", "false");

			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) 
					&& startBrowser ) {
				// Give server a moment to fully start before opening browser
				Thread.sleep(1000);
				URI uri = new URI("http://localhost:" + serverPort);
				Desktop.getDesktop().browse(uri);
				log.info("🌐 Browser opened: {}", uri);
			} else {
				log.debug("Desktop browsing not supported on this platform");
			}
		} catch (Exception e) {
			log.debug("Failed to auto-open browser: {}", e.getMessage());
		}
	}

	private boolean isContainerEnvironment() {
		// Check for Docker container indicators
		return java.nio.file.Files.exists(java.nio.file.Paths.get("/.dockerenv"))
				|| System.getenv("CONTAINER") != null
				|| System.getenv("KUBERNETES_SERVICE_HOST") != null
				|| "true".equals(System.getProperty("java.awt.headless")); 
	}
}