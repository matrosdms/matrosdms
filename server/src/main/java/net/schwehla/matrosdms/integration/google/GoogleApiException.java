/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.integration.google;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A failed Google call. {@link #isRetryable()} decides whether the sync job is retried
 * with backoff (network, 5xx, rate limits, expired access token) or given up and the
 * error shown on the profile page (bad request, API not enabled, missing scope).
 *
 * <p>No stack trace: the message carries Google's own explanation, and an offline
 * laptop would otherwise log a full trace on every retry.
 */
public class GoogleApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private static final Set<String> RATE_LIMIT_REASONS = Set.of(
			"rateLimitExceeded", "userRateLimitExceeded", "quotaExceeded", "backendError");

	/** HTTP status, or 0 when Google was not reachable at all. */
	private final int status;
	private final boolean retryable;
	/** OAuth error code from the token endpoint, e.g. "invalid_grant". */
	private final String oauthError;

	public GoogleApiException(int status, String message, boolean retryable) {
		this(status, message, retryable, null, null);
	}

	public GoogleApiException(int status, String message, boolean retryable, String oauthError, Throwable cause) {
		super(message, cause, false, false);
		this.status = status;
		this.retryable = retryable;
		this.oauthError = oauthError;
	}

	public static GoogleApiException fromResponse(int status, String body, ObjectMapper mapper) {
		String message = "HTTP " + status;
		String reason = null;
		String oauthError = null;
		try {
			JsonNode root = mapper.readTree(body == null ? "" : body);
			JsonNode error = root.path("error");
			if (error.isTextual()) {
				// OAuth token endpoint: {"error":"invalid_grant","error_description":"..."}
				oauthError = error.asText();
				message = oauthError + ": " + root.path("error_description").asText("");
			} else if (error.isObject()) {
				// Google APIs: {"error":{"code":403,"message":"...","errors":[{"reason":"..."}]}}
				message = error.path("message").asText(message);
				reason = error.path("errors").path(0).path("reason").asText(null);
			}
		} catch (Exception ignored) {
			// not JSON - keep the status line
		}
		boolean transientFailure = status == 408 || status == 429 || status >= 500;
		boolean retryable = oauthError != null
				// Token endpoint: invalid_client / invalid_grant / unauthorized_client are
				// configuration or consent problems that no retry fixes
				? transientFailure
				// APIs: a 401 means the access token expired - refreshed on the retry
				: transientFailure || status == 401
						|| (status == 403 && reason != null && RATE_LIMIT_REASONS.contains(reason));
		return new GoogleApiException(status, "Google API " + status + ": " + message, retryable, oauthError, null);
	}

	public int getStatus() {
		return status;
	}

	public boolean isRetryable() {
		return retryable;
	}

	/** The remote task/event (or its list/calendar) no longer exists. */
	public boolean isNotFound() {
		return status == 404 || status == 410;
	}

	/** The refresh token was revoked or expired: only a new consent helps. */
	public boolean isInvalidGrant() {
		return "invalid_grant".equals(oauthError);
	}
}
