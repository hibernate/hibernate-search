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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;

/**
 * A delegating {@link SearchPredicateContainerContext}.
 * <p>
 * Mainly useful when implementing a {@link SearchPredicateContainerContextExtension}.
 */
public class DelegatingSearchPredicateContainerContextImpl implements SearchPredicateContainerContext {

	private final SearchPredicateContainerContext delegate;

	public DelegatingSearchPredicateContainerContextImpl(SearchPredicateContainerContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public MatchAllPredicateContext matchAll() {
		return delegate.matchAll();
	}

	@Override
	public BooleanJunctionPredicateContext bool() {
		return delegate.bool();
	}

	@Override
	public SearchPredicateTerminalContext bool(Consumer<? super BooleanJunctionPredicateContext> clauseContributor) {
		return delegate.bool( clauseContributor );
	}

	@Override
	public MatchPredicateContext match() {
		return delegate.match();
	}

	@Override
	public RangePredicateContext range() {
		return delegate.range();
	}

	@Override
	public NestedPredicateContext nested() {
		return delegate.nested();
	}

	@Override
	public SpatialPredicateContext spatial() {
		return delegate.spatial();
	}

	@Override
	public void predicate(SearchPredicate predicate) {
		delegate.predicate( predicate );
	}

	@Override
	public <T> T extension(SearchPredicateContainerContextExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchPredicateContainerExtensionContext extension() {
		return delegate.extension();
	}

	protected SearchPredicateContainerContext getDelegate() {
		return delegate;
	}
}
