/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in an "nested" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @deprecated Use {@link SearchPredicateFactory#nested(String)} instead.
 */
@Deprecated
public interface NestedPredicateOptionsStep<S extends NestedPredicateOptionsStep<?>>
		extends PredicateFinalStep {

}
