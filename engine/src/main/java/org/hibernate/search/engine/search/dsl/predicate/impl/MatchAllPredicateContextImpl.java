/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


class MatchAllPredicateContextImpl<N, B>
		implements MatchAllPredicateContext<N>, SearchPredicateContributor<B> {

	private final SearchPredicateFactory<?, B> factory;
	private final Supplier<N> nextContextProvider;

	private final MatchAllPredicateBuilder<B> matchAllBuilder;
	private MatchAllExceptContext exceptContext;

	MatchAllPredicateContextImpl(SearchPredicateFactory<?, B> factory,
			Supplier<N> nextContextProvider) {
		this.factory = factory;
		this.nextContextProvider = nextContextProvider;
		this.matchAllBuilder = factory.matchAll();
	}

	@Override
	public MatchAllPredicateContext<N> boostedTo(float boost) {
		this.matchAllBuilder.boost( boost );
		return this;
	}

	@Override
	public MatchAllPredicateContext<N> except(SearchPredicate searchPredicate) {
		getExceptContext().containerContext.predicate( searchPredicate );
		return this;
	}

	@Override
	public MatchAllPredicateContext<N> except(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( getExceptContext().containerContext );
		return this;
	}

	@Override
	public B contribute() {
		if ( exceptContext != null ) {
			return exceptContext.contribute();
		}
		else {
			return matchAllBuilder.toImplementation();
		}
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}

	private MatchAllExceptContext getExceptContext() {
		if ( exceptContext == null ) {
			exceptContext = new MatchAllExceptContext();
		}
		return exceptContext;
	}

	private class MatchAllExceptContext implements SearchPredicateDslContext<MatchAllPredicateContext<N>, B>,
			SearchPredicateContributor<B> {

		private final BooleanJunctionPredicateBuilder<B> booleanBuilder;
		private final List<SearchPredicateContributor<? extends B>> exceptClauseContributors = new ArrayList<>();

		private final SearchPredicateContainerContextImpl<MatchAllPredicateContext<N>, B> containerContext;

		MatchAllExceptContext() {
			this.booleanBuilder = MatchAllPredicateContextImpl.this.factory.bool();
			this.booleanBuilder.must( matchAllBuilder.toImplementation() );
			this.containerContext = new SearchPredicateContainerContextImpl<>(
					MatchAllPredicateContextImpl.this.factory, this
			);
		}

		@Override
		public void addChild(SearchPredicateContributor<? extends B> contributor) {
			exceptClauseContributors.add( contributor );
		}

		@Override
		public MatchAllPredicateContext<N> getNextContext() {
			return MatchAllPredicateContextImpl.this;
		}

		@Override
		public B contribute() {
			for ( SearchPredicateContributor<? extends B> contributor : exceptClauseContributors ) {
				booleanBuilder.mustNot( contributor.contribute() );
			}
			return booleanBuilder.toImplementation();
		}

	}
}
