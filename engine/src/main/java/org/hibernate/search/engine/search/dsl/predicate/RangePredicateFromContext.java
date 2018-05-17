/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


/**
 * The context used when defining a range predicate,
 * after the lower bound was provided but before the upper bound was provided.
 *
 * @param <N> The type of the next context (returned by {@link #to(Object)} for example).
 */
public interface RangePredicateFromContext<N> {

	default N to(Object value) {
		return to( value, RangeBoundInclusion.INCLUDED );
	}

	N to(Object value, RangeBoundInclusion inclusion);

}
