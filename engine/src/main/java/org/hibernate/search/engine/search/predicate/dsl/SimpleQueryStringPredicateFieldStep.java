/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.SimpleQueryStringPredicateFieldReference;

/**
 * The initial step in a "simple query string" predicate definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <N> The type of the next step.
 */
public interface SimpleQueryStringPredicateFieldStep<SR, N extends SimpleQueryStringPredicateFieldMoreStep<SR, ?, ?>>
		extends CommonQueryStringPredicateFieldStep<SR, N, SimpleQueryStringPredicateFieldReference<SR, ?>> {

}
