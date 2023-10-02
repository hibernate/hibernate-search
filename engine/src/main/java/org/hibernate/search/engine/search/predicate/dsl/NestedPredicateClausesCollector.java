/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * An object where the <a href="SimpleBooleanPredicateClausesCollector.html#clauses">clauses</a>
 * of a {@link SearchPredicateFactory#nested(String) nested predicate} can be set.
 * <p>
 * The resulting nested predicate must match <em>all</em> inner clauses,
 * similarly to an {@link SearchPredicateFactory#and() "and" predicate}.
 */
public interface NestedPredicateClausesCollector<S extends NestedPredicateClausesCollector<?>>
		extends SimpleBooleanPredicateClausesCollector<S> {

}
