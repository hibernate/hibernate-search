/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchIdPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtensionContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateContext;

/**
 * A delegating {@link SearchPredicateFactoryContext}.
 * <p>
 * Mainly useful when implementing a {@link SearchPredicateFactoryContextExtension}.
 */
public class DelegatingSearchPredicateFactoryContext implements SearchPredicateFactoryContext {

	private final SearchPredicateFactoryContext delegate;

	public DelegatingSearchPredicateFactoryContext(SearchPredicateFactoryContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public MatchAllPredicateContext matchAll() {
		return delegate.matchAll();
	}

	@Override
	public MatchIdPredicateContext id() {
		return delegate.id();
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
	public PhrasePredicateContext phrase() {
		return delegate.phrase();
	}

	@Override
	public WildcardPredicateContext wildcard() {
		return delegate.wildcard();
	}

	@Override
	public NestedPredicateContext nested() {
		return delegate.nested();
	}

	@Override
	public SimpleQueryStringPredicateContext simpleQueryString() {
		return delegate.simpleQueryString();
	}

	@Override
	public SpatialPredicateContext spatial() {
		return delegate.spatial();
	}

	@Override
	public <T> T extension(SearchPredicateFactoryContextExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchPredicateFactoryExtensionContext extension() {
		return delegate.extension();
	}

	protected SearchPredicateFactoryContext getDelegate() {
		return delegate;
	}
}
