/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.dsl.ExplicitEndContext;

/**
 * The context used when defining a match predicate, after at least one field was mentioned.
 *
 * @param <N> The type of the next context (returned by {@link #matching(Object)}).
 */
public interface MatchPredicateFieldSetContext<N> extends MultiFieldPredicateFieldSetContext<MatchPredicateFieldSetContext<N>> {

	default MatchPredicateFieldSetContext<N> orField(String field) {
		return orFields( field );
	}

	MatchPredicateFieldSetContext<N> orFields(String ... field);

	ExplicitEndContext<N> matching(Object value);

}
