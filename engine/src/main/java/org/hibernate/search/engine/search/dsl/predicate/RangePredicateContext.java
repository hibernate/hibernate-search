/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when starting to define a range predicate.
 *
 * @param <N> The type of the next context (returned by {@link RangePredicateFieldSetContext#above(Object)}
 * or {@link RangePredicateFromContext#to(Object)} for example).
 */
public interface RangePredicateContext<N> {

	default RangePredicateFieldSetContext<N> onField(String fieldName) {
		return onFields( fieldName );
	}

	RangePredicateFieldSetContext<N> onFields(String ... fieldName);

}
