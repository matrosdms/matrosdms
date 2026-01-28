/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import net.schwehla.matrosdms.config.model.AppServerSpringConfig;
import net.schwehla.matrosdms.domain.action.MAction;

@Service
public class PluginManager {

	private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

	@Autowired
	AppServerSpringConfig appConfig;

	private final List<ActionSyncPlugin> actionPlugins = new ArrayList<>();

	@EventListener(ApplicationReadyEvent.class)
	public void loadPlugins() {
		String pluginPathStr = appConfig.getServer().getPlugins().getPath();
		if (pluginPathStr == null)
			return;

		Path pluginDir = Paths.get(pluginPathStr);
		if (!Files.exists(pluginDir)) {
			try {
				Files.createDirectories(pluginDir);
			} catch (IOException e) {
			}
			return;
		}

		try (Stream<Path> stream = Files.list(pluginDir)) {
			List<URL> jarUrls = new ArrayList<>();
			stream
					.filter(p -> p.toString().endsWith(".jar"))
					.forEach(
							p -> {
								try {
									jarUrls.add(p.toUri().toURL());
									log.info("🔌 Found Plugin JAR: {}", p.getFileName());
								} catch (Exception e) {
									log.error("Invalid JAR path", e);
								}
							});

			if (jarUrls.isEmpty())
				return;

			// Create a child ClassLoader for the plugins
			URLClassLoader childClassLoader = new URLClassLoader(
					jarUrls.toArray(new URL[0]), this.getClass().getClassLoader() // Parent is current App
			);

			// Use Java ServiceLoader to find implementations
			ServiceLoader<ActionSyncPlugin> loader = ServiceLoader.load(ActionSyncPlugin.class, childClassLoader);
			for (ActionSyncPlugin plugin : loader) {
				log.info("🔌 Loaded Action Plugin: {}", plugin.getName());
				actionPlugins.add(plugin);
			}

		} catch (Exception e) {
			log.error("Failed to load plugins", e);
		}
	}

	public void notifyActionCreated(MAction action) {
		actionPlugins.forEach(p -> safeExec(() -> p.onActionCreated(action)));
	}

	public void notifyActionUpdated(MAction action) {
		actionPlugins.forEach(p -> safeExec(() -> p.onActionUpdated(action)));
	}

	public void notifyActionDeleted(String uuid) {
		actionPlugins.forEach(p -> safeExec(() -> p.onActionDeleted(uuid)));
	}

	private void safeExec(Runnable r) {
		try {
			r.run();
		} catch (Exception e) {
			log.error("Plugin Execution Failed", e);
		}
	}
}
