/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import net.schwehla.matrosdms.domain.storage.EStorageLocation;

@Configuration
@ConfigurationProperties("app")
public class AppServerSpringConfig {

	private Server server = new Server();
	private Processing processing = new Processing();
	private AiConfig ai = new AiConfig();
	private TemplateConfig templates = new TemplateConfig();

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Processing getProcessing() {
		return processing;
	}

	public void setProcessing(Processing processing) {
		this.processing = processing;
	}

	public AiConfig getAi() {
		return ai;
	}

	public void setAi(AiConfig ai) {
		this.ai = ai;
	}

	public TemplateConfig getTemplates() {
		return templates;
	}

	public void setTemplates(TemplateConfig templates) {
		this.templates = templates;
	}

	public static class TemplateConfig {
		private String location = "classpath*:templates/local/";

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}
	}

	public static class AiConfig {
		private ClassificationConfig classification = new ClassificationConfig();
		private StrategyConfig chat = new StrategyConfig();
		private EmbeddingConfig embedding = new EmbeddingConfig();

		public ClassificationConfig getClassification() {
			return classification;
		}

		public void setClassification(ClassificationConfig classification) {
			this.classification = classification;
		}

		public StrategyConfig getChat() {
			return chat;
		}

		public void setChat(StrategyConfig chat) {
			this.chat = chat;
		}

		public EmbeddingConfig getEmbedding() {
			return embedding;
		}

		public void setEmbedding(EmbeddingConfig embedding) {
			this.embedding = embedding;
		}
	}

	public static class EmbeddingConfig {
		private String url;
		private String model;
		private int dimension = 768;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public int getDimension() {
			return dimension;
		}

		public void setDimension(int dimension) {
			this.dimension = dimension;
		}
	}

	public static class ClassificationConfig {
		private StrategyConfig ollama = new StrategyConfig();
		private StrategyConfig heuristic = new StrategyConfig();

		public StrategyConfig getOllama() {
			return ollama;
		}

		public void setOllama(StrategyConfig ollama) {
			this.ollama = ollama;
		}

		public StrategyConfig getHeuristic() {
			return heuristic;
		}

		public void setHeuristic(StrategyConfig heuristic) {
			this.heuristic = heuristic;
		}
	}

	public static class StrategyConfig {
		private boolean enabled = true;
		private int preference = 100;
		private String url;
		private String model;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getPreference() {
			return preference;
		}

		public void setPreference(int preference) {
			this.preference = preference;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}
	}

	public static class Processing {
		private boolean convertTextToPdf = true;
		private int concurrency = 2;

		public boolean isConvertTextToPdf() {
			return convertTextToPdf;
		}

		public void setConvertTextToPdf(boolean convertTextToPdf) {
			this.convertTextToPdf = convertTextToPdf;
		}

		public int getConcurrency() {
			return concurrency;
		}

		public void setConcurrency(int concurrency) {
			this.concurrency = concurrency;
		}
	}

	public static class Server {
		Cache cache = new Cache();
		Inbox inbox = new Inbox();
		// REMOVED: Upload upload = new Upload();
		Processed processed = new Processed();
		Inbox ignored = new Inbox();
		Inbox temp = new Inbox();
		Plugins plugins = new Plugins();
		List<StoreElement> store = new ArrayList<>();

		public Cache getCache() {
			return cache;
		}

		public void setCache(Cache cache) {
			this.cache = cache;
		}

		public List<StoreElement> getStore() {
			return store;
		}

		public void setStore(List<StoreElement> store) {
			this.store = store;
		}

		public Inbox getInbox() {
			return inbox;
		}

		public void setInbox(Inbox inbox) {
			this.inbox = inbox;
		}

		public Processed getProcessed() {
			return processed;
		}

		public void setProcessed(Processed processed) {
			this.processed = processed;
		}

		public Inbox getIgnored() {
			return ignored;
		}

		public void setIgnored(Inbox ignored) {
			this.ignored = ignored;
		}

		public Inbox getTemp() {
			return temp;
		}

		public void setTemp(Inbox temp) {
			this.temp = temp;
		}

		public Plugins getPlugins() {
			return plugins;
		}

		public void setPlugins(Plugins plugins) {
			this.plugins = plugins;
		}
	}

	public static class Inbox {
		boolean crypted;
		String path;

		public boolean isCrypted() {
			return crypted;
		}

		public void setCrypted(boolean crypted) {
			this.crypted = crypted;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}

	public static class Cache extends Inbox {
	}

	public static class Processed extends Inbox {
	}

	public static class Plugins extends Inbox {
	}

	public static class StoreElement {
		String id;
		String path;
		String password;
		String salt;
		String cryptor;
		EStorageLocation type;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public EStorageLocation getType() {
			return type;
		}

		public void setType(EStorageLocation type) {
			this.type = type;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getCryptor() {
			return cryptor;
		}

		public void setCryptor(String cryptor) {
			this.cryptor = cryptor;
		}

		public String getSalt() {
			return salt;
		}

		public void setSalt(String salt) {
			this.salt = salt;
		}
	}
}
