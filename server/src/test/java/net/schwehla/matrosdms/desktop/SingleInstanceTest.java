/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class SingleInstanceTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
		System.clearProperty("server.port");
	}

	private URI serve(int status, String body) throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/actuator/info", exchange -> {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
		return URI.create("http://localhost:" + server.getAddress().getPort() + "/actuator/info");
	}

	@Test
	void recognizesRunningMatros() throws Exception {
		URI info = serve(200, "{\"build\":{\"group\":\"net.schwehla.matrosdms\",\"artifact\":\"server\"}}");
		assertThat(SingleInstance.isMatros(info)).isTrue();
	}

	@Test
	void foreignServerOnThePortIsNotMatros() throws Exception {
		URI info = serve(200, "{\"app\":\"something-else\"}");
		assertThat(SingleInstance.isMatros(info)).isFalse();
	}

	@Test
	void errorResponseIsNotMatros() throws Exception {
		URI info = serve(404, "matrosdms"); // body must not rescue a non-200 answer
		assertThat(SingleInstance.isMatros(info)).isFalse();
	}

	@Test
	void nothingListeningIsNotMatros() throws Exception {
		int freePort;
		try (ServerSocket socket = new ServerSocket(0)) {
			freePort = socket.getLocalPort();
		}
		assertThat(SingleInstance.isMatrosOnPort(freePort)).isFalse();
	}

	@Test
	void portPrecedenceMatchesSpring() {
		// Default without any source.
		assertThat(SingleInstance.resolvePort(new String[0])).isEqualTo(9090);

		// The system property (what ProfileManager publishes for the active profile).
		System.setProperty("server.port", "9091");
		assertThat(SingleInstance.resolvePort(new String[0])).isEqualTo(9091);

		// A program argument outranks the property, an unparsable one is ignored.
		assertThat(SingleInstance.resolvePort(new String[] { "--server.port=9092" })).isEqualTo(9092);
		assertThat(SingleInstance.resolvePort(new String[] { "--server.port=oops" })).isEqualTo(9091);
	}
}
