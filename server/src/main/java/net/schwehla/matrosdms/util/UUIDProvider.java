/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.util;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class UUIDProvider {

	private final BaseX baseX = new BaseX(BaseX.DICTIONARY_32);

	// EPOCH: Jan 1, 2025 00:00:00 UTC
	private static final long CUSTOM_EPOCH = 1735689600000L;

	private static final long RANDOM_BOUND = 1000L;
	private static final long SEQUENCE_BOUND = 100L;

	private static final BigInteger SEQ_MULTIPLIER = BigInteger.valueOf(RANDOM_BOUND);
	private static final BigInteger TIME_MULTIPLIER = BigInteger.valueOf(SEQUENCE_BOUND * RANDOM_BOUND);

	private long lastTimestamp = -1L;
	private long sequence = 0L;

	public synchronized String getTimeBasedUUID() {
		long currentTimestamp = System.currentTimeMillis();

		if (currentTimestamp < lastTimestamp)
			currentTimestamp = lastTimestamp;

		if (currentTimestamp == lastTimestamp) {
			sequence++;
			if (sequence >= SEQUENCE_BOUND) {
				currentTimestamp = waitNextMillis(lastTimestamp);
				sequence = 0;
			}
		} else {
			sequence = 0;
		}

		lastTimestamp = currentTimestamp;

		long effectiveTime = Math.max(0, currentTimestamp - CUSTOM_EPOCH);
		long randomPart = ThreadLocalRandom.current().nextLong(RANDOM_BOUND);

		BigInteger uniqueVal = BigInteger.valueOf(effectiveTime)
				.multiply(TIME_MULTIPLIER)
				.add(BigInteger.valueOf(sequence).multiply(SEQ_MULTIPLIER))
				.add(BigInteger.valueOf(randomPart));

		return baseX.encode(uniqueVal);
	}

	public String extractInfo(String uuid) {
		try {
			BigInteger value = baseX.decode(uuid);
			BigInteger[] split1 = value.divideAndRemainder(SEQ_MULTIPLIER);
			long randomPart = split1[1].longValue();
			BigInteger remaining = split1[0];
			BigInteger[] split2 = remaining.divideAndRemainder(BigInteger.valueOf(SEQUENCE_BOUND));
			long sequencePart = split2[1].longValue();
			long timeDiff = split2[0].longValue();
			long originalTimestamp = timeDiff + CUSTOM_EPOCH;
			String dateString = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(originalTimestamp));
			return String.format(
					"UUID: %s | Date: %s | Seq: %d | Rand: %d", uuid, dateString, sequencePart, randomPart);
		} catch (Exception e) {
			return "Invalid UUID format";
		}
	}

	// NEW: Check if a string is likely one of our UUIDs
	public boolean isValid(String uuid) {
		if (uuid == null || uuid.isBlank())
			return false;
		// Our Dictionary 32 does not contain 0,1, O, I.
		// A quick regex check for invalid chars:
		for (char c : uuid.toCharArray()) {
			boolean valid = false;
			for (char d : BaseX.DICTIONARY_32) {
				if (c == d) {
					valid = true;
					break;
				}
			}
			if (!valid)
				return false;
		}
		return true;
	}

	private long waitNextMillis(long lastTimestamp) {
		long timestamp = System.currentTimeMillis();
		while (timestamp <= lastTimestamp) {
			timestamp = System.currentTimeMillis();
		}
		return timestamp;
	}
}
