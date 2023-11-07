/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Defines a function to calculate the vector similarity, i.e. distance between two vectors.
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
	INNER_PRODUCT,
	/**
	 * Cosine similarity. Distance is calculated as {@code d(x,y) = 1 - \sum_(i=1) ^(n) x_i*y_i / ( \sqrt( \sum_(i=1) ^(n) x_i^2 ) \sqrt( \sum_(i=1) ^(n) y_i^2 ) },
	 * similarity function is {@code s = 1 / (1+d) }, but may differ between backends.
	 */
	COSINE;
}
