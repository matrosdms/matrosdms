package net.schwehla.matrosdms.service.mail.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import net.schwehla.matrosdms.service.domain.UserService;

class MailAuthenticatorTest {

	private UserService userService;
	private MailAuthenticator auth;

	@BeforeEach
	void setUp() {
		userService = Mockito.mock(UserService.class);
		auth = new MailAuthenticator();
		ReflectionTestUtils.setField(auth, "userService", userService);
	}

	@Test
	void acceptsCorrectCredentials() {
		when(userService.login(eq("bob"), eq("secret"))).thenReturn(null);
		assertThat(auth.authenticate("bob", "secret")).isTrue();
	}

	@Test
	void rejectsWhenUserServiceThrows() {
		when(userService.login(eq("bob"), eq("wrong")))
				.thenThrow(new BadCredentialsException("nope"));
		assertThat(auth.authenticate("bob", "wrong")).isFalse();
	}

	@Test
	void rejectsBlankOrNull() {
		assertThat(auth.authenticate(null, "x")).isFalse();
		assertThat(auth.authenticate("", "x")).isFalse();
		assertThat(auth.authenticate("bob", null)).isFalse();
	}

	@Test
	void authenticatePlainDecodesSaslBlob() {
		when(userService.login(eq("bob"), eq("secret"))).thenReturn(null);
		String blob = Base64.getEncoder().encodeToString(
				"\0bob\0secret".getBytes(StandardCharsets.UTF_8));
		assertThat(auth.authenticatePlain(blob)).isTrue();
	}

	@Test
	void authenticatePlainRejectsMalformedBase64() {
		assertThat(auth.authenticatePlain("!!!not base64!!!")).isFalse();
	}

	@Test
	void decodeBase64IsNullSafe() {
		assertThat(auth.decodeBase64(null)).isNull();
		assertThat(auth.decodeBase64("###")).isNull();
		assertThat(auth.decodeBase64(Base64.getEncoder().encodeToString("hi".getBytes())))
				.isEqualTo("hi");
	}
}
