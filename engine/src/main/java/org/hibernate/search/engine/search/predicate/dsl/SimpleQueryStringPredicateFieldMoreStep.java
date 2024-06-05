/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.SimpleQueryStringPredicateFieldReference;

/**
 * The step in a "simple query string" predicate definition where the query string to match can be set
 * (see the superinterface {@link SimpleQueryStringPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface SimpleQueryStringPredicateFieldMoreStep<
		SR,
		S extends SimpleQueryStringPredicateFieldMoreStep<SR, ?, N>,
		N extends SimpleQueryStringPredicateOptionsStep<?>>
		extends CommonQueryStringPredicateFieldMoreStep<SR, S, N, SimpleQueryStringPredicateFieldReference<SR, ?>> {

}
