package net.schwehla.matrosdms.service.mail.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class MimeHelperTest {

	private static final Pattern IMAP_DATE = Pattern.compile(
			"\\d{2}-\\w{3}-\\d{4} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4}");

	@Test
	void producesImapFormattedDate() throws Exception {
		Path tmp = Files.createTempFile("mime-test", ".eml");
		try {
			String date = MimeHelper.getInternalDate(tmp);
			assertThat(date).isNotBlank();
			assertThat(IMAP_DATE.matcher(date).matches())
					.as("date '%s' should match IMAP internaldate format", date)
					.isTrue();
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	@Test
	void isThreadSafeUnderConcurrentAccess() throws Exception {
		// Regression: the old static SimpleDateFormat was not thread-safe and
		// produced garbled output / exceptions under concurrent IMAP FETCHes.
		Path tmp = Files.createTempFile("mime-test", ".eml");
		try {
			ExecutorService pool = Executors.newFixedThreadPool(8);
			List<Future<String>> futures = new ArrayList<>();
			for (int i = 0; i < 200; i++) {
				futures.add(pool.submit(() -> MimeHelper.getInternalDate(tmp)));
			}
			for (Future<String> f : futures) {
				assertThat(IMAP_DATE.matcher(f.get()).matches()).isTrue();
			}
			pool.shutdown();
		} finally {
			Files.deleteIfExists(tmp);
		}
	}
}
