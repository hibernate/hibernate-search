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

import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;

class MultiFieldPredicateCommonState<N, CTX, C, F extends MultiFieldPredicateCommonState.FieldSetContext<CTX, C>>
		implements SearchPredicateContributor<CTX, C> {

	private final SearchPredicateFactory<CTX, C> factory;

	private final Supplier<N> nextContextProvider;

	private final List<F> fieldSetContexts = new ArrayList<>();

	MultiFieldPredicateCommonState(SearchPredicateFactory<CTX, C> factory, Supplier<N> nextContextProvider) {
		this.factory = factory;
		this.nextContextProvider = nextContextProvider;
	}

	public SearchPredicateFactory<CTX, C> getFactory() {
		return factory;
	}

	public void add(F fieldSetContext) {
		fieldSetContexts.add( fieldSetContext );
	}

	public Supplier<N> getNextContextProvider() {
		return nextContextProvider;
	}

	List<F> getFieldSetContexts() {
		return fieldSetContexts;
	}

	@Override
	public void contribute(CTX context, C collector) {
		List<SearchPredicateBuilder<CTX, ? super C>> predicateBuilders = new ArrayList<>();
		for ( F fieldSetContext : fieldSetContexts ) {
			fieldSetContext.contributePredicateBuilders( predicateBuilders::add );
		}
		if ( predicateBuilders.size() > 1 ) {
			BooleanJunctionPredicateBuilder<CTX, C> boolBuilder = factory.bool();
			for ( SearchPredicateBuilder<CTX, ? super C> predicateBuilder : predicateBuilders ) {
				boolBuilder.should( predicateBuilder );
			}
			boolBuilder.contribute( context, collector );
		}
		else {
			predicateBuilders.get( 0 ).contribute( context, collector );
		}
	}

	public interface FieldSetContext<CTX, C> {
		void contributePredicateBuilders(Consumer<SearchPredicateBuilder<CTX, ? super C>> collector);
	}
}