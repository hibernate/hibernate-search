/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface KnnPredicateBuilder {

	void k(int k);

	void vector(Object vector);

	void filter(SearchPredicate filter);

	/**
	 * @return An implementation-specific view of this builder,
	 * allowing the backend to call a {@code build()} method in particular.
	 */
	SearchPredicate build();
}
