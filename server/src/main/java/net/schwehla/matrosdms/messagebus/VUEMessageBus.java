/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.messagebus;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.schwehla.matrosdms.domain.api.BroadcastMessage;
import net.schwehla.matrosdms.domain.api.EBroadcastSource;
import net.schwehla.matrosdms.domain.api.EBroadcastType;

@Service
public class VUEMessageBus {

	private static final Logger log = LoggerFactory.getLogger(VUEMessageBus.class);

	private final List<SseEmitter> localEmitters = new CopyOnWriteArrayList<>();
	private final ScheduledExecutorService heartbeatScheduler = new ScheduledThreadPoolExecutor(1);

	@Autowired
	private ObjectMapper objectMapper;

	@PostConstruct
	public void init() {
		// Heartbeat every 15s
		heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 15, 15, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void shutdown() {
		heartbeatScheduler.shutdownNow();
	}

	public void addEmitter(SseEmitter emitter) {
		this.localEmitters.add(emitter);

		emitter.onCompletion(() -> this.localEmitters.remove(emitter));
		emitter.onTimeout(
				() -> {
					emitter.complete();
					this.localEmitters.remove(emitter);
				});
		emitter.onError(
				(e) -> {
					// Log at debug only, client disconnects are normal
					log.debug("SSE Client Disconnected: {}", e.getMessage());
					this.localEmitters.remove(emitter);
				});
	}

	public void sendMessageToGUI(EBroadcastSource process, EBroadcastType type, Object payload) {
		try {
			BroadcastMessage msg = new BroadcastMessage(payload);
			msg.setProcess(process);
			msg.setType(type);

			String jsonPayload = objectMapper.writeValueAsString(msg);

			for (SseEmitter emitter : localEmitters) {
				try {
					emitter.send(SseEmitter.event().data(jsonPayload));
				} catch (IOException | IllegalStateException e) {
					// Client is gone, remove silently
					this.localEmitters.remove(emitter);
				}
			}

		} catch (Exception e) {
			log.error("SSE Serialization Error", e);
		}
	}

	private void sendHeartbeat() {
		if (localEmitters.isEmpty())
			return;

		for (SseEmitter emitter : localEmitters) {
			try {
				emitter.send(SseEmitter.event().comment("ping"));
			} catch (IOException | IllegalStateException e) {
				// FIX: Do not log stack trace for heartbeat failures.
				// This indicates the tab was closed.
				this.localEmitters.remove(emitter);
			}
		}
	}
}
