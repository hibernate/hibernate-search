/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.dsl.predicate.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.dsl.predicate.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtensionStep;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldStep;

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
	public MatchAllPredicateOptionsStep matchAll() {
		return delegate.matchAll();
	}

	@Override
	public MatchIdPredicateMatchingStep id() {
		return delegate.id();
	}

	@Override
	public BooleanPredicateClausesStep bool() {
		return delegate.bool();
	}

	@Override
	public PredicateFinalStep bool(Consumer<? super BooleanPredicateClausesStep> clauseContributor) {
		return delegate.bool( clauseContributor );
	}

	@Override
	public MatchPredicateFieldStep match() {
		return delegate.match();
	}

	@Override
	public RangePredicateFieldStep range() {
		return delegate.range();
	}

	@Override
	public PhrasePredicateFieldStep phrase() {
		return delegate.phrase();
	}

	@Override
	public WildcardPredicateFieldStep wildcard() {
		return delegate.wildcard();
	}

	@Override
	public NestedPredicateFieldStep nested() {
		return delegate.nested();
	}

	@Override
	public SimpleQueryStringPredicateFieldStep simpleQueryString() {
		return delegate.simpleQueryString();
	}

	@Override
	public ExistsPredicateFieldStep exists() {
		return delegate.exists();
	}

	@Override
	public SpatialPredicateInitialStep spatial() {
		return delegate.spatial();
	}

	@Override
	public <T> T extension(SearchPredicateFactoryContextExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchPredicateFactoryContextExtensionStep extension() {
		return delegate.extension();
	}

	protected SearchPredicateFactoryContext getDelegate() {
		return delegate;
	}
}
