/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.dsl.spi.SearchPredicateContributor;

class MultiFieldPredicateCommonState<N, C, F extends MultiFieldPredicateCommonState.FieldSetContext<C>>
		implements SearchPredicateContributor<C> {

	private final SearchTargetContext<C> targetContext;

	private final Supplier<N> nextContextProvider;

	private final List<F> fieldSetContexts = new ArrayList<>();

	public MultiFieldPredicateCommonState(SearchTargetContext<C> targetContext, Supplier<N> nextContextProvider) {
		this.targetContext = targetContext;
		this.nextContextProvider = nextContextProvider;
	}

	public SearchTargetContext<C> getTargetContext() {
		return targetContext;
	}

	public void add(F fieldSetContext) {
		fieldSetContexts.add( fieldSetContext );
	}

	public Supplier<N> getNextContextProvider() {
		return nextContextProvider;
	}

	protected List<F> getFieldSetContexts() {
		return fieldSetContexts;
	}

	@Override
	public void contribute(C collector) {
		List<SearchPredicateBuilder<? super C>> predicateBuilders = new ArrayList<>();
		for ( F fieldSetContext : fieldSetContexts ) {
			fieldSetContext.contributePredicateBuilders( predicateBuilders::add );
		}
		if ( predicateBuilders.size() > 1 ) {
			BooleanJunctionPredicateBuilder<C> boolBuilder = targetContext.getSearchPredicateFactory().bool();
			C shouldCollector = boolBuilder.getShouldCollector();
			predicateBuilders.forEach( b -> b.contribute( shouldCollector ) );
			boolBuilder.contribute( collector );
		}
		else {
			predicateBuilders.get( 0 ).contribute( collector );
		}
	}

	public interface FieldSetContext<C> {
		void contributePredicateBuilders(Consumer<SearchPredicateBuilder<? super C>> collector);
	}
}