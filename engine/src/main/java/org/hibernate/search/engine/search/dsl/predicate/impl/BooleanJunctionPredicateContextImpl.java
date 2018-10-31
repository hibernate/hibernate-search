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
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class BooleanJunctionPredicateContextImpl<B>
		extends AbstractSearchPredicateTerminalContext<B>
		implements BooleanJunctionPredicateContext {

	private final SearchPredicateFactoryContext factoryContext;

	private final BooleanJunctionPredicateBuilder<B> builder;

	private final MinimumShouldMatchContextImpl<BooleanJunctionPredicateContext> minimumShouldMatchContext;

	private final OccurContext must;
	private final OccurContext mustNot;
	private final OccurContext should;
	private final OccurContext filter;

	BooleanJunctionPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory, SearchPredicateFactoryContext factoryContext) {
		super( factory );
		this.factoryContext = factoryContext;
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
		must.addClause( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext mustNot(SearchPredicate searchPredicate) {
		mustNot.addClause( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext should(SearchPredicate searchPredicate) {
		should.addClause( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext filter(SearchPredicate searchPredicate) {
		filter.addClause( searchPredicate );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext must(
			Function<? super SearchPredicateFactoryContext, SearchPredicate> clauseContributor) {
		must.addClause( clauseContributor );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext mustNot(
			Function<? super SearchPredicateFactoryContext, SearchPredicate> clauseContributor) {
		mustNot.addClause( clauseContributor );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext should(
			Function<? super SearchPredicateFactoryContext, SearchPredicate> clauseContributor) {
		should.addClause( clauseContributor );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext filter(
			Function<? super SearchPredicateFactoryContext, SearchPredicate> clauseContributor) {
		filter.addClause( clauseContributor );
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
	protected B toImplementation() {
		must.contribute( builder::must );
		mustNot.contribute( builder::mustNot );
		should.contribute( builder::should );
		filter.contribute( builder::filter );
		return builder.toImplementation();
	}

	private class OccurContext {

		private List<B> clauseBuilders;

		OccurContext() {
		}

		void addClause(Function<? super SearchPredicateFactoryContext, SearchPredicate> clauseContributor) {
			addClause( clauseContributor.apply( factoryContext ) );
		}

		void addClause(SearchPredicate predicate) {
			if ( clauseBuilders == null ) {
				clauseBuilders = new ArrayList<>();
			}
			clauseBuilders.add( factory.toImplementation( predicate ) );
		}

		void contribute(Consumer<B> builderCollector) {
			if ( clauseBuilders != null ) {
				for ( B builder : clauseBuilders ) {
					builderCollector.accept( builder );
				}
			}
		}
	}

}
