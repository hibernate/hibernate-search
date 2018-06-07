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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.Contracts;


class BooleanJunctionPredicateContextImpl<N, C>
		implements BooleanJunctionPredicateContext<N>, SearchPredicateContributor<C> {

	private final SearchPredicateFactory<C> factory;

	private final Supplier<N> nextContextProvider;

	private final BooleanJunctionPredicateBuilder<C> builder;

	private final OccurContext must;
	private final OccurContext mustNot;
	private final OccurContext should;
	private final OccurContext filter;

	BooleanJunctionPredicateContextImpl(SearchPredicateFactory<C> factory,
			Supplier<N> nextContextProvider) {
		this.factory = factory;
		this.nextContextProvider = nextContextProvider;
		this.builder = factory.bool();
		this.must = new OccurContext();
		this.mustNot = new OccurContext();
		this.should = new OccurContext();
		this.filter = new OccurContext();
	}

	@Override
	public BooleanJunctionPredicateContext<N> boostedTo(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> must(SearchPredicate searchPredicate) {
		return must().predicate( searchPredicate );
	}

	@Override
	public BooleanJunctionPredicateContext<N> mustNot(SearchPredicate searchPredicate) {
		return mustNot().predicate( searchPredicate );
	}

	@Override
	public BooleanJunctionPredicateContext<N> should(SearchPredicate searchPredicate) {
		return should().predicate( searchPredicate );
	}

	@Override
	public BooleanJunctionPredicateContext<N> filter(SearchPredicate searchPredicate) {
		return filter().predicate( searchPredicate );
	}

	@Override
	public SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> must() {
		return must.containerContext;
	}

	@Override
	public SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> mustNot() {
		return mustNot.containerContext;
	}

	@Override
	public SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> should() {
		return should.containerContext;
	}

	@Override
	public SearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> filter() {
		return filter.containerContext;
	}

	@Override
	public BooleanJunctionPredicateContext<N> must(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( must() );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> mustNot(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( mustNot() );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> should(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( should() );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> filter(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor) {
		clauseContributor.accept( filter() );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> minimumShouldMatchNumber(int ignoreConstraintCeiling,
			int matchingClausesNumber) {
		Contracts.assertPositiveOrZero( ignoreConstraintCeiling, "ignoreConstraintCeiling" );
		builder.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> minimumShouldMatchRatio(int ignoreConstraintCeiling,
			double matchingClausesRatio) {
		Contracts.assertPositiveOrZero( ignoreConstraintCeiling, "ignoreConstraintCeiling" );
		builder.minimumShouldMatchRatio( ignoreConstraintCeiling, matchingClausesRatio );
		return this;
	}

	@Override
	public void contribute(C collector) {
		must.contribute( builder.getMustCollector() );
		mustNot.contribute( builder.getMustNotCollector() );
		should.contribute( builder.getShouldCollector() );
		filter.contribute( builder.getFilterCollector() );
		builder.contribute( collector );
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}

	private class OccurContext implements SearchPredicateDslContext<BooleanJunctionPredicateContext<N>, C>,
			SearchPredicateContributor<C> {

		private final List<SearchPredicateContributor<C>> children = new ArrayList<>();

		private final SearchPredicateContainerContextImpl<BooleanJunctionPredicateContext<N>, C> containerContext;

		OccurContext() {
			this.containerContext = new SearchPredicateContainerContextImpl<>(
					BooleanJunctionPredicateContextImpl.this.factory, this );
		}

		@Override
		public void addContributor(SearchPredicateContributor<C> child) {
			children.add( child );
		}

		@Override
		public BooleanJunctionPredicateContext<N> getNextContext() {
			return BooleanJunctionPredicateContextImpl.this;
		}

		@Override
		public void contribute(C collector) {
			children.forEach( c -> c.contribute( collector ) );
		}

	}

}
