/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * An object where the <a href="SimpleBooleanPredicateClausesCollector.html#clauses">clauses</a>
 * of a {@link TypedSearchPredicateFactory#nested(String) nested predicate} can be set.
 * <p>
 * The resulting nested predicate must match <em>all</em> inner clauses,
 * similarly to an {@link TypedSearchPredicateFactory#and() "and" predicate}.
 *
 * @param <SR> Scope root type.
 */
public interface NestedPredicateClausesCollector<SR, S extends NestedPredicateClausesCollector<SR, ?>>
		extends SimpleBooleanPredicateClausesCollector<SR, S> {

}
