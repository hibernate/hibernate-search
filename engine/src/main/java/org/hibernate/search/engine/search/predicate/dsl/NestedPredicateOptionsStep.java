/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in an "nested" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @deprecated Use {@link TypedSearchPredicateFactory#nested(String)} instead.
 */
@Deprecated(since = "6.2")
public interface NestedPredicateOptionsStep<S extends NestedPredicateOptionsStep<?>>
		extends PredicateFinalStep {

}
