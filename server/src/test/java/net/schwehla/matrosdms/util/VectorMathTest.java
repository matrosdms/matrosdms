package net.schwehla.matrosdms.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VectorMathTest {

	@Test
	void bytesRoundTrip() {
		float[] original = { 0.0f, 1.5f, -2.25f, 3.14159f };
		float[] restored = VectorMath.fromBytes(VectorMath.toBytes(original));
		assertThat(restored).containsExactly(original);
	}

	@Test
	void cosineOfIdenticalVectorsIsOne() {
		float[] v = { 1f, 2f, 3f };
		assertThat(VectorMath.cosine(v, v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
	}

	@Test
	void cosineOfOrthogonalVectorsIsZero() {
		assertThat(VectorMath.cosine(new float[] { 1f, 0f }, new float[] { 0f, 1f }))
				.isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
	}

	@Test
	void cosineOfOppositeVectorsIsNegativeOne() {
		assertThat(VectorMath.cosine(new float[] { 1f, 1f }, new float[] { -1f, -1f }))
				.isCloseTo(-1.0, org.assertj.core.data.Offset.offset(1e-6));
	}

	@Test
	void cosineWithZeroVectorIsZeroNotNaN() {
		assertThat(VectorMath.cosine(new float[] { 0f, 0f }, new float[] { 1f, 1f }))
				.isEqualTo(0.0);
	}
}
