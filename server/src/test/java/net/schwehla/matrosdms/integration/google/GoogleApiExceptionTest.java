package net.schwehla.matrosdms.integration.google;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class GoogleApiExceptionTest {

	private final ObjectMapper mapper = new ObjectMapper();

	private GoogleApiException parse(int status, String body) {
		return GoogleApiException.fromResponse(status, body, mapper);
	}

	@Test
	void serverErrorsAndRateLimitsAreRetried() {
		assertThat(parse(503, "").isRetryable()).isTrue();
		assertThat(parse(429, "{}").isRetryable()).isTrue();
		assertThat(parse(403, """
				{"error":{"code":403,"message":"Rate Limit Exceeded","errors":[{"reason":"userRateLimitExceeded"}]}}
				""").isRetryable()).isTrue();
	}

	@Test
	void expiredAccessTokenIsRetried() {
		assertThat(parse(401, "{\"error\":{\"code\":401,\"message\":\"Invalid Credentials\"}}").isRetryable())
				.isTrue();
	}

	@Test
	void apiNotEnabledIsPermanentAndKeepsGooglesExplanation() {
		GoogleApiException e = parse(403, """
				{"error":{"code":403,"message":"Google Tasks API has not been used in project 123 before or it is disabled.",
				"errors":[{"reason":"accessNotConfigured"}]}}
				""");
		assertThat(e.isRetryable()).isFalse();
		assertThat(e.getMessage()).contains("Google Tasks API has not been used");
	}

	@Test
	void revokedRefreshTokenIsRecognised() {
		GoogleApiException e = parse(400, "{\"error\":\"invalid_grant\",\"error_description\":\"Token has been expired or revoked.\"}");
		assertThat(e.isInvalidGrant()).isTrue();
		assertThat(e.isRetryable()).isFalse();
	}

	@Test
	void wrongClientSecretIsNotRetriedForDays() {
		GoogleApiException e = parse(401, "{\"error\":\"invalid_client\",\"error_description\":\"Unauthorized\"}");
		assertThat(e.isRetryable()).isFalse();
		assertThat(e.getMessage()).contains("invalid_client");
	}

	@Test
	void tokenEndpointOutageIsRetried() {
		assertThat(parse(503, "{\"error\":\"temporarily_unavailable\"}").isRetryable()).isTrue();
	}

	@Test
	void goneCountsAsNotFound() {
		assertThat(parse(404, "").isNotFound()).isTrue();
		assertThat(parse(410, "").isNotFound()).isTrue();
		assertThat(parse(400, "").isNotFound()).isFalse();
	}

	@Test
	void pkceChallengeMatchesRfc7636Example() {
		// RFC 7636 appendix B
		assertThat(GoogleOAuthClient.codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
				.isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
	}
}
