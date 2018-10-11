/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerExtensionContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;

/**
 * A delegating {@link SearchPredicateContainerContext}.
 * <p>
 * Mainly useful when implementing a {@link SearchPredicateContainerContextExtension}.
 */
public class DelegatingSearchPredicateContainerContextImpl<N> implements SearchPredicateContainerContext<N> {

	private final SearchPredicateContainerContext<N> delegate;

	public DelegatingSearchPredicateContainerContextImpl(SearchPredicateContainerContext<N> delegate) {
		this.delegate = delegate;
	}

	@Override
	public MatchAllPredicateContext<N> matchAll() {
		return delegate.matchAll();
	}

	@Override
	public BooleanJunctionPredicateContext<N> bool() {
		return delegate.bool();
	}

	@Override
	public N bool(Consumer<? super BooleanJunctionPredicateContext<?>> clauseContributor) {
		return delegate.bool( clauseContributor );
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
	public NestedPredicateContext<N> nested() {
		return delegate.nested();
	}

	@Override
	public SpatialPredicateContext<N> spatial() {
		return delegate.spatial();
	}

	@Override
	public N predicate(SearchPredicate predicate) {
		return delegate.predicate( predicate );
	}

	@Override
	public <T> T extension(SearchPredicateContainerContextExtension<N, T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchPredicateContainerExtensionContext<N> extension() {
		return delegate.extension();
	}

	protected SearchPredicateContainerContext<N> getDelegate() {
		return delegate;
	}
}
