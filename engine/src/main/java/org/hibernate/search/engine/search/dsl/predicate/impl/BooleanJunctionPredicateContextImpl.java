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

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractObjectCreatingSearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


class BooleanJunctionPredicateContextImpl<B>
		extends AbstractObjectCreatingSearchPredicateContributor<B>
		implements BooleanJunctionPredicateContext, SearchPredicateContributor<B> {

	private final BooleanJunctionPredicateBuilder<B> builder;

	private final MinimumShouldMatchContextImpl<BooleanJunctionPredicateContext> minimumShouldMatchContext;

	private final OccurContext must;
	private final OccurContext mustNot;
	private final OccurContext should;
	private final OccurContext filter;

	BooleanJunctionPredicateContextImpl(SearchPredicateFactory<?, B> factory) {
		super( factory );
		this.builder = factory.bool();
		this.must = new OccurContext();
		this.mustNot = new OccurContext();
		this.should = new OccurContext();
		this.filter = new OccurContext();
		this.minimumShouldMatchContext = new MinimumShouldMatchContextImpl<>( builder, this );
	}

	@Override
	public BooleanJunctionPredicateContext boostedTo(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext must(SearchPredicate searchPredicate) {
		must.containerContext.predicate( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext mustNot(SearchPredicate searchPredicate) {
		mustNot.containerContext.predicate( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext should(SearchPredicate searchPredicate) {
		should.containerContext.predicate( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext filter(SearchPredicate searchPredicate) {
		filter.containerContext.predicate( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext must(Consumer<? super SearchPredicateContainerContext> clauseContributor) {
		clauseContributor.accept( must.containerContext );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext mustNot(Consumer<? super SearchPredicateContainerContext> clauseContributor) {
		clauseContributor.accept( mustNot.containerContext );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext should(Consumer<? super SearchPredicateContainerContext> clauseContributor) {
		clauseContributor.accept( should.containerContext );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext filter(Consumer<? super SearchPredicateContainerContext> clauseContributor) {
		clauseContributor.accept( filter.containerContext );
		return this;
	}

	@Override
	public MinimumShouldMatchContext<? extends BooleanJunctionPredicateContext> minimumShouldMatch() {
		return minimumShouldMatchContext;
	}

	@Override
	public BooleanJunctionPredicateContext minimumShouldMatch(
			Consumer<? super MinimumShouldMatchContext<?>> constraintContributor) {
		constraintContributor.accept( minimumShouldMatchContext );
		return this;
	}

	@Override
	protected B doContribute() {
		must.contribute( builder::must );
		mustNot.contribute( builder::mustNot );
		should.contribute( builder::should );
		filter.contribute( builder::filter );
		return builder.toImplementation();
	}

	private class OccurContext implements SearchPredicateDslContext<B> {

		private final SearchPredicateContainerContextImpl<B> containerContext;

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

		void contribute(Consumer<B> builderCollector) {
			if ( clauseContributors != null ) {
				for ( SearchPredicateContributor<? extends B> contributor : clauseContributors ) {
					builderCollector.accept( contributor.contribute() );
				}
			}
		}
	}

}
