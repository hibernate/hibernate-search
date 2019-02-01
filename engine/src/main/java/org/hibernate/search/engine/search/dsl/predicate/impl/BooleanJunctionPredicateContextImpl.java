/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class BooleanJunctionPredicateContextImpl<B>
		extends AbstractSearchPredicateTerminalContext<B>
		implements BooleanJunctionPredicateContext {

	private final SearchPredicateFactoryContext factoryContext;

	private final BooleanJunctionPredicateBuilder<B> builder;

	private final MinimumShouldMatchContextImpl<BooleanJunctionPredicateContext> minimumShouldMatchContext;

	BooleanJunctionPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory, SearchPredicateFactoryContext factoryContext) {
		super( factory );
		this.factoryContext = factoryContext;
		this.builder = factory.bool();
		this.minimumShouldMatchContext = new MinimumShouldMatchContextImpl<>( builder, this );
	}

	@Override
	public BooleanJunctionPredicateContext boostedTo(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext must(SearchPredicate searchPredicate) {
		builder.must( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext mustNot(SearchPredicate searchPredicate) {
		builder.mustNot( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext should(SearchPredicate searchPredicate) {
		builder.should( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext filter(SearchPredicate searchPredicate) {
		builder.filter( factory.toImplementation( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext must(
			Function<? super SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> clauseContributor) {
		must( clauseContributor.apply( factoryContext ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext mustNot(
			Function<? super SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> clauseContributor) {
		mustNot( clauseContributor.apply( factoryContext ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext should(
			Function<? super SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> clauseContributor) {
		should( clauseContributor.apply( factoryContext ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext filter(
			Function<? super SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> clauseContributor) {
		filter( clauseContributor.apply( factoryContext ) );
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
		return builder.toImplementation();
	}

}
