/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Defines a function to calculate the vector similarity, i.e. distance between two vectors.
 * <p>
 * Note, some backends or their distributions may not support all of available similarity options.
 */
@Incubating
public enum VectorSimilarity {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * L2 (Euclidean) norm. Distance is calculated as {@code d(x,y) = \sum_(i=1) ^(n) (x_i - y_i)^2 } and similarity function is {@code s = 1 / (1+d) }
	 */
	L2,
	/**
	 * Inner product (dot product in particular). Distance is calculated as {@code d(x,y) = \sum_(i=1) ^(n) x_i*y_i },
	 * similarity function is {@code s = 1 / (1+d) }, but may differ between backends.
	 * <p>
	 * <strong>WARNING:</strong>: to use this similarity efficiently, both index and search vectors <b>must</b> be normalized,
	 * otherwise search may produce poor results.
	 * Floating point vectors must be <a href="https://en.wikipedia.org/wiki/Unit_vector">normalized to be of unit length</a>,
	 * while byte vectors should simply all have the same norm.
	 */
	DOT_PRODUCT,
	/**
	 * Cosine similarity. Distance is calculated as {@code d(x,y) = 1 - \sum_(i=1) ^(n) x_i*y_i / ( \sqrt( \sum_(i=1) ^(n) x_i^2 ) \sqrt( \sum_(i=1) ^(n) y_i^2 ) },
	 * similarity function is {@code s = 1 / (1+d) }, but may differ between backends.
	 */
	COSINE,

	/**
	 * Similar to {@link VectorSimilarity#DOT_PRODUCT} but does not require vector normalization.
	 * Distance is calculated as {@code d(x,y) = \sum_(i=1) ^(n) (x_i - y_i)^2 }
	 * and similarity function is {@code d < 0 ? s = 1 / (1 - d)  : s = d + 1}
	 */
	MAX_INNER_PRODUCT;
}
