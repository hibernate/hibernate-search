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
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


class BooleanJunctionPredicateContextImpl<N, B>
		implements BooleanJunctionPredicateContext<N>, SearchPredicateContributor<B> {

	private final SearchPredicateFactory<?, B> factory;

	private final Supplier<N> nextContextProvider;

	private final BooleanJunctionPredicateBuilder<B> builder;

	private final MinimumShouldMatchContextImpl<BooleanJunctionPredicateContext<N>> minimumShouldMatchContext;

	private final OccurContext must;
	private final OccurContext mustNot;
	private final OccurContext should;
	private final OccurContext filter;

	BooleanJunctionPredicateContextImpl(SearchPredicateFactory<?, B> factory,
			Supplier<N> nextContextProvider) {
		this.factory = factory;
		this.nextContextProvider = nextContextProvider;
		this.builder = factory.bool();
		this.must = new OccurContext();
		this.mustNot = new OccurContext();
		this.should = new OccurContext();
		this.filter = new OccurContext();
		this.minimumShouldMatchContext = new MinimumShouldMatchContextImpl<>( builder, this );
	}

	@Override
	public BooleanJunctionPredicateContext<N> boostedTo(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> must(SearchPredicate searchPredicate) {
		return must.containerContext.predicate( searchPredicate );
	}

	@Override
	public BooleanJunctionPredicateContext<N> mustNot(SearchPredicate searchPredicate) {
		return mustNot.containerContext.predicate( searchPredicate );
	}

	@Override
	public BooleanJunctionPredicateContext<N> should(SearchPredicate searchPredicate) {
		return should.containerContext.predicate( searchPredicate );
	}

	@Override
	public BooleanJunctionPredicateContext<N> filter(SearchPredicate searchPredicate) {
		return filter.containerContext.predicate( searchPredicate );
	}

	@Override
	public BooleanJunctionPredicateContext<N> must(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( must.containerContext );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> mustNot(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( mustNot.containerContext );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> should(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( should.containerContext );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> filter(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( filter.containerContext );
		return this;
	}

	@Override
	public MinimumShouldMatchContext<? extends BooleanJunctionPredicateContext<N>> minimumShouldMatch() {
		return minimumShouldMatchContext;
	}

	@Override
	public BooleanJunctionPredicateContext<N> minimumShouldMatch(
			Consumer<? super MinimumShouldMatchContext<?>> constraintContributor) {
		constraintContributor.accept( minimumShouldMatchContext );
		return this;
	}

	@Override
	public B contribute() {
		must.contribute( builder::must );
		mustNot.contribute( builder::mustNot );
		should.contribute( builder::should );
		filter.contribute( builder::filter );
		return builder.toImplementation();
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}

	private class OccurContext implements SearchPredicateDslContext<BooleanJunctionPredicateContext<N>, B> {

		private final SearchPredicateContainerContextImpl<BooleanJunctionPredicateContext<N>, B> containerContext;

		private List<SearchPredicateContributor<? extends B>> clauseContributors;

		OccurContext() {
			this.containerContext = new SearchPredicateContainerContextImpl<>(
					BooleanJunctionPredicateContextImpl.this.factory, this
			);
		}

		@Override
		public void addChild(SearchPredicateContributor<? extends B> contributor) {
			if ( clauseContributors == null ) {
				clauseContributors = new ArrayList<>();
			}
			clauseContributors.add( contributor );
		}

		@Override
		public BooleanJunctionPredicateContext<N> getNextContext() {
			return BooleanJunctionPredicateContextImpl.this;
		}

		void contribute(Consumer<B> builderCollector) {
			if ( clauseContributors != null ) {
				for ( SearchPredicateContributor<? extends B> contributor : clauseContributors ) {
					builderCollector.accept( contributor.contribute() );
				}
			}
		}
	}

}
