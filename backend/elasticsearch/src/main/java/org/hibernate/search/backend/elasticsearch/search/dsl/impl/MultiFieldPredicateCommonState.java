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

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.BooleanJunctionPredicateBuilder;

import com.google.gson.JsonObject;

class MultiFieldPredicateCommonState<N, F extends MultiFieldPredicateCommonState.FieldSetContext>
		implements ElasticsearchSearchPredicateContributor {

	private final SearchTargetContext targetContext;

	private final Supplier<N> nextContextProvider;

	private final List<F> fieldSetContexts = new ArrayList<>();

	public MultiFieldPredicateCommonState(SearchTargetContext targetContext, Supplier<N> nextContextProvider) {
		this.targetContext = targetContext;
		this.nextContextProvider = nextContextProvider;
	}

	public SearchTargetContext getTargetContext() {
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
	public void contribute(Consumer<JsonObject> collector) {
		List<JsonObject> queries = new ArrayList<>();
		for ( F fieldSetContext : fieldSetContexts ) {
			fieldSetContext.contribute( queries::add );
		}
		if ( queries.size() > 1 ) {
			BooleanJunctionPredicateBuilder boolBuilder = targetContext.getSearchPredicateFactory().bool();
			queries.stream().forEach( boolBuilder::should );
			collector.accept( boolBuilder.build() );
		}
		else {
			collector.accept( queries.get( 0 ) );
		}
	}

	public interface FieldSetContext {
		void contribute(Consumer<JsonObject> collector);
	}
}