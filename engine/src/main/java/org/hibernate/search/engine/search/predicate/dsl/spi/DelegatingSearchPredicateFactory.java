/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.predicate.dsl.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchIdPredicateMatchingStep;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.MatchPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.PhrasePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldStep;

/**
 * A delegating {@link SearchPredicateFactory}.
 * <p>
 * Mainly useful when implementing a {@link SearchPredicateFactoryExtension}.
 */
public class DelegatingSearchPredicateFactory implements SearchPredicateFactory {

	private final SearchPredicateFactory delegate;

	public DelegatingSearchPredicateFactory(SearchPredicateFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public MatchAllPredicateOptionsStep<?> matchAll() {
		return delegate.matchAll();
	}

	@Override
	public MatchIdPredicateMatchingStep<?> id() {
		return delegate.id();
	}

	@Override
	public BooleanPredicateClausesStep<?> bool() {
		return delegate.bool();
	}

	@Override
	public PredicateFinalStep bool(Consumer<? super BooleanPredicateClausesStep<?>> clauseContributor) {
		return delegate.bool( clauseContributor );
	}

	@Override
	public MatchPredicateFieldStep<?> match() {
		return delegate.match();
	}

	@Override
	public RangePredicateFieldStep<?> range() {
		return delegate.range();
	}

	@Override
	public PhrasePredicateFieldStep<?> phrase() {
		return delegate.phrase();
	}

	@Override
	public WildcardPredicateFieldStep<?> wildcard() {
		return delegate.wildcard();
	}

	@Override
	public NestedPredicateFieldStep<?> nested() {
		return delegate.nested();
	}

	@Override
	public SimpleQueryStringPredicateFieldStep<?> simpleQueryString() {
		return delegate.simpleQueryString();
	}

	@Override
	public ExistsPredicateFieldStep<?> exists() {
		return delegate.exists();
	}

	@Override
	public SpatialPredicateInitialStep spatial() {
		return delegate.spatial();
	}

	@Override
	public <T> T extension(SearchPredicateFactoryExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchPredicateFactoryExtensionIfSupportedStep extension() {
		return delegate.extension();
	}

	protected SearchPredicateFactory getDelegate() {
		return delegate;
	}
}
