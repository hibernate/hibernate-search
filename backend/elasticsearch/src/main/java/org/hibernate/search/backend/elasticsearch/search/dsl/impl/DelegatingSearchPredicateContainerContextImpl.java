/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.spi.SearchPredicateContainerContextExtension;

public class DelegatingSearchPredicateContainerContextImpl<N> implements SearchPredicateContainerContext<N> {

	private final SearchPredicateContainerContext<N> delegate;

	public DelegatingSearchPredicateContainerContextImpl(SearchPredicateContainerContext<N> delegate) {
		this.delegate = delegate;
	}

	@Override
	public BooleanJunctionPredicateContext<N> bool() {
		return delegate.bool();
	}

	@Override
	public MatchPredicateContext<N> match() {
		return delegate.match();
	}

	@Override
	public RangePredicateContext<N> range() {
		return delegate.range();
	}

	@Override
	public N predicate(SearchPredicate predicate) {
		return delegate.predicate( predicate );
	}

	@Override
	public <T> T withExtension(SearchPredicateContainerContextExtension<N, T> extension) {
		return delegate.withExtension( extension );
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension,
			Consumer<T> clauseContributor) {
		return delegate.withExtensionOptional( extension, clauseContributor );
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension,
			Consumer<T> clauseContributor,
			Consumer<SearchPredicateContainerContext<N>> fallbackClauseContributor) {
		return delegate.withExtensionOptional( extension, clauseContributor, fallbackClauseContributor );
	}
}
