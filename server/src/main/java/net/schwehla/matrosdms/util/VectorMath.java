/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.util;

import java.nio.ByteBuffer;

public final class VectorMath {

	private VectorMath() {
	}

	public static byte[] toBytes(float[] vector) {
		ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES);
		for (float v : vector) {
			buffer.putFloat(v);
		}
		return buffer.array();
	}

	public static float[] fromBytes(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		float[] vector = new float[bytes.length / Float.BYTES];
		for (int i = 0; i < vector.length; i++) {
			vector[i] = buffer.getFloat();
		}
		return vector;
	}

	/** Cosine similarity in [-1, 1]; 0 when either vector has zero magnitude. */
	public static double cosine(float[] a, float[] b) {
		double dot = 0, normA = 0, normB = 0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
			normA += a[i] * a[i];
			normB += b[i] * b[i];
		}
		if (normA == 0 || normB == 0) {
			return 0;
		}
		return dot / (Math.sqrt(normA) * Math.sqrt(normB));
	}
}
