/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial step in a "query string" predicate definition, where the target field can be set.
 *
 * @param <N> The type of the next step.
 */
public interface QueryStringPredicateFieldStep<N extends QueryStringPredicateFieldMoreStep<?, ?>>
		extends CommonQueryStringPredicateFieldStep<N> {

}
