/*
 * Copyright (c) ${year} Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import net.schwehla.matrosdms.domain.core.EArchiveFilter;
import net.schwehla.matrosdms.domain.core.MCategory;
import net.schwehla.matrosdms.domain.core.MContext;
import net.schwehla.matrosdms.domain.core.MItem;
import net.schwehla.matrosdms.domain.core.MStore;
import net.schwehla.matrosdms.domain.core.MUser;
import net.schwehla.matrosdms.domain.inbox.InboxFile;
import net.schwehla.matrosdms.service.message.CreateCategoryMessage;
import net.schwehla.matrosdms.service.message.CreateContextMessage;
import net.schwehla.matrosdms.service.message.CreateUserMessage;

@SpringBootApplication
public class Testclient implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(Testclient.class);

	public static void main(String[] args) {
		LOG.info("STARTING THE CLIENT");
		SpringApplication.run(Testclient.class, args);
		LOG.info("CLIENT FINISHED");
	}

	@Override
	public void run(String... args) {

		RestClient restClient = createRestClient();

		MCategory rootWhoDSebug = restClient.get()
				// IMPORTANT: Add transitive=true to fetch children
				.uri("/category/ROOT_WHO?transitive=true")
				.retrieve()
				.body(MCategory.class);

		// 1. Create User
		CreateUserMessage message = new CreateUserMessage();
		message.setFirstname("Admin");
		message.setName("admin_" + System.currentTimeMillis()); // Ensure unique name
		message.setEmail("admin@example.com"); // Added required field
		message.setPassword("admin");

		MUser savedUser = restClient.post()
				.uri("/user")
				.body(message)
				.retrieve()
				.body(MUser.class);

		LOG.info("✅ Created User: " + savedUser.getUuid());

		// 2. Create Store
		MStore store = new MStore();
		store.setShortname("Test");
		store.setName("ms");

		MStore savedStore = restClient.post()
				.uri("/store")
				.body(store)
				.retrieve()
				.body(MStore.class);

		LOG.info("✅ Created Store: " + savedStore.getUuid());

		// 3. Create Category under ROOT_WHO
		CreateCategoryMessage category = new CreateCategoryMessage();
		category.setName("WER_" + System.currentTimeMillis());
		category.setDescription("Test Category");

		MCategory catResult = restClient.post()
				.uri("/category/ROOT_WHO")
				.body(category)
				.retrieve()
				.body(MCategory.class);

		LOG.info("✅ Created Category: " + catResult.getName() + " -> " + catResult.getUuid());

		// --- DEBUG: LOAD CATEGORY (ROOT_WHO) ---
		LOG.info("--- DEBUGGING ROOT_WHO ---");

		MCategory rootWho = restClient.get()
				// IMPORTANT: Add transitive=true to fetch children
				.uri("/category/ROOT_WHO?transitive=true")
				.retrieve()
				.body(MCategory.class);

		LOG.info("🔍 Root Name: " + rootWho.getName());
		if (rootWho.getChildren() != null && !rootWho.getChildren().isEmpty()) {
			LOG.info("🔍 Children Count: " + rootWho.getChildren().size());
			Object firstChild = rootWho.getChildren().get(0);

			// TYPE SAFETY CHECK:
			if (firstChild instanceof MCategory) {
				LOG.info("✅ TYPE SAFETY PASS: Child is mapped to MCategory object: "
						+ ((MCategory) firstChild).getName());
			} else if (firstChild instanceof String) {
				LOG.error("❌ TYPE SAFETY FAIL: Child is a String (UUID): " + firstChild);
			} else {
				LOG.warn("⚠️  Child is raw Map (LinkedHashMap). Jackson config might be missing info: "
						+ firstChild.getClass().getName());
			}
		} else {
			LOG.warn("⚠️  No children found for ROOT_WHO");
		}
		LOG.info("--------------------------");

		// 4. Create Context
		CreateContextMessage context = new CreateContextMessage();
		context.setName("Test Context");
		context.setDescription("Test");
		context.getCategoryList().add(catResult.getUuid());

		MContext cRestult = restClient.post()
				.uri("/context")
				.body(context)
				.retrieve()
				.body(MContext.class);

		LOG.info("✅ Created Context: " + cRestult.getUuid());

		// 5. Upload Files
		InboxFile file = uploadRandomFile(restClient);
		LOG.info("✅ Uploaded File 1: " + file.getSha256());

		// 6. Get Items
		List<MItem> loadInfoItemList = restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/items")
						.queryParam("context", cRestult.getUuid())
						.queryParam("archiveState", EArchiveFilter .Names.ACTIVE_ONLY)
						.build())
				.retrieve()
				.body(List.class);

		LOG.info("✅ Loaded ItemList: " + loadInfoItemList.size() + " items.");

		// Example of "TypeSafe" List fetching using ParameterizedTypeReference
		// (If you were fetching a raw List<MItem> instead of MItemList object)
		/*
		 * List<MUser> userList = restClient.get()
		 * .uri("/users")
		 * .retrieve()
		 * .body(new ParameterizedTypeReference<List<MUser>>() {});
		 */
	}

	private InboxFile uploadRandomFile(RestClient restClient) {
		String pathTmp = System.getProperty("java.io.tmpdir") + "/file_" + System.currentTimeMillis() + ".txt";

		try {
			Files.write(Paths.get(pathTmp), "content".getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			LOG.error("Failed to create temp file", e);
			return null;
		}

		var fileSystemResource = new FileSystemResource(pathTmp);

		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", fileSystemResource);

		InboxFile file = restClient.post()
				.uri("/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA) // Set content type for the request
				.body(form)
				.retrieve()
				.body(InboxFile.class);
		return file;
	}

	private RestClient createRestClient() {
		// Updated Base URL to include /api prefix based on your WebConfig
		return RestClient.builder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory())
				.baseUrl("http://localhost:9090/api")
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.messageConverters(configurer -> configurer
						.add(new org.springframework.http.converter.FormHttpMessageConverter())) // Needed for Multipart
				.build();
	}
}