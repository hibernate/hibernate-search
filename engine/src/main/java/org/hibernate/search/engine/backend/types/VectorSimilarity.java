/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * TODO: vector : docs
 */
@Incubating
public enum VectorSimilarity {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	L2, INNER_PRODUCT, COSINE;
}
