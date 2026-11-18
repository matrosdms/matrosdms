/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.journal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.entity.management.DBUser;

class ChangeJournalFilterTest {

	private final List<String> lines = new ArrayList<>();
	private final ObjectMapper json = new ObjectMapper();

	@AfterEach
	void clearUser() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void aCreateIsJournaledWithItsRequestAndTheIdTheServerGaveIt() throws Exception {
		DBUser martin = new DBUser();
		martin.setName("martin");
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(martin, null));

		MockHttpServletResponse response = run("POST", "/api/items",
				"{\"name\":\"VBL 2015\",\"contextUuid\":\"49GKRQ8BV6W\",\"issueDate\":\"2016-08-30\"}",
				201, "{\"uuid\":\"49K2P2MC8PX\",\"name\":\"VBL 2015\"}");

		assertThat(lines).hasSize(1);
		JsonNode line = json.readTree(lines.get(0));
		assertThat(line.get("user").asText()).isEqualTo("martin");
		assertThat(line.get("method").asText()).isEqualTo("POST");
		assertThat(line.get("path").asText()).isEqualTo("/api/items");
		assertThat(line.get("status").asInt()).isEqualTo(201);
		assertThat(line.get("time").asText()).matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d.*");
		assertThat(line.at("/request/contextUuid").asText()).isEqualTo("49GKRQ8BV6W");
		assertThat(line.at("/response/uuid").asText()).isEqualTo("49K2P2MC8PX");
		// the client still gets the answer
		assertThat(response.getContentAsString()).isEqualTo("{\"uuid\":\"49K2P2MC8PX\",\"name\":\"VBL 2015\"}");
	}

	@Test
	void movesDeletesAndOtherChangesAreJournaled() throws Exception {
		run("POST", "/api/items/batch/move", "{\"itemUuids\":[\"A\"],\"targetContextUuid\":\"C\"}", 200, "");
		run("PUT", "/api/contexts/49GKRQ8BV6W", "{\"name\":\"VBL\"}", 200, "{}");
		run("DELETE", "/api/items/49K2P2MC8PX", "", 204, "");
		run("POST", "/api/category/48AD2UATU59", "{\"name\":\"Rente\"}", 201, "{}");
		run("POST", "/api/attribute-type", "{\"name\":\"Betrag\"}", 201, "{}");
		run("PUT", "/api/actions/X", "{\"status\":\"DONE\"}", 200, "{}");

		assertThat(lines).hasSize(6);
		assertThat(json.readTree(lines.get(2)).has("request")).isFalse();
	}

	@Test
	void readsFailuresAndOtherAreasAreNotJournaled() throws Exception {
		run("GET", "/api/items/49K2P2MC8PX", "", 200, "{}");
		run("POST", "/api/search", "{\"query\":\"vbl\"}", 200, "[]");
		run("POST", "/api/items/49K2P2MC8PX/ai/transform", "{}", 200, "{}");
		run("POST", "/api/auth/login", "{\"username\":\"martin\",\"password\":\"secret\"}", 200, "{}");
		run("PUT", "/api/users/1", "{\"password\":\"secret\"}", 200, "{}");
		run("PUT", "/api/contexts/49GKRQ8BV6W", "{\"name\":\"\"}", 400, "{\"message\":\"invalid\"}");
		run("POST", "/api/items", "{}", 500, "");

		assertThat(lines).isEmpty();
	}

	@Test
	void secretsAreBlankedOutAndEveryEntryIsOneLine() throws Exception {
		run("POST", "/api/actions",
				"{\"title\":\"Call\\nback\",\"meta\":{\"password\":\"p\",\"apiToken\":\"t\"},\"list\":[{\"clientSecret\":\"s\"}]}",
				201, "{\"uuid\":\"X\"}");

		assertThat(lines).singleElement().asString().doesNotContain("\n").doesNotContain("\"p\"")
				.doesNotContain("\"t\"").doesNotContain("\"s\"");
		JsonNode line = json.readTree(lines.get(0));
		assertThat(line.at("/request/meta/password").asText()).isEqualTo("***");
		assertThat(line.at("/request/meta/apiToken").asText()).isEqualTo("***");
		assertThat(line.at("/request/list/0/clientSecret").asText()).isEqualTo("***");
		assertThat(line.at("/request/title").asText()).isEqualTo("Call\nback");
	}

	@Test
	void anOversizedBodyIsKeptCutOffAndFlagged() throws Exception {
		String big = "{\"description\":\"" + "x".repeat(ChangeJournalFilter.MAX_BODY) + "\"}";

		run("PUT", "/api/items/49K2P2MC8PX", big, 200, "{}");

		JsonNode line = json.readTree(lines.get(0));
		assertThat(line.get("requestTruncated").asBoolean()).isTrue();
		assertThat(line.get("request").asText()).hasSize(ChangeJournalFilter.MAX_BODY);
	}

	@Test
	void aJournalThatCannotBeWrittenDoesNotFailTheChange() throws Exception {
		ChangeJournalFilter filter = new ChangeJournalFilter(true, l -> {
			throw new IllegalStateException("disk full");
		});

		MockHttpServletResponse response = run(filter, "POST", "/api/items", "{}", 201, "{\"uuid\":\"X\"}");

		assertThat(response.getStatus()).isEqualTo(201);
		assertThat(response.getContentAsString()).isEqualTo("{\"uuid\":\"X\"}");
	}

	@Test
	void switchedOffItWritesNothing() throws Exception {
		run(new ChangeJournalFilter(false, lines::add), "POST", "/api/items", "{}", 201, "{}");

		assertThat(lines).isEmpty();
	}

	private MockHttpServletResponse run(String method, String path, String body, int status, String answer)
			throws Exception {
		return run(new ChangeJournalFilter(true, lines::add), method, path, body, status, answer);
	}

	/** Runs the filter around a handler that reads the body, as Spring MVC does, and answers. */
	private static MockHttpServletResponse run(ChangeJournalFilter filter, String method, String path, String body,
			int status, String answer) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		if (!body.isEmpty()) {
			request.setContentType("application/json");
			request.setContent(body.getBytes(StandardCharsets.UTF_8));
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpServlet handler = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
				req.getInputStream().readAllBytes();
				res.setStatus(status);
				res.getOutputStream().write(answer.getBytes(StandardCharsets.UTF_8));
			}
		};
		filter.doFilter(request, response, new MockFilterChain(handler));
		return response;
	}
}
