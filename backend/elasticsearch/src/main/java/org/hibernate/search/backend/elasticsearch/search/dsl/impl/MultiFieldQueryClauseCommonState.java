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

import org.hibernate.search.backend.elasticsearch.search.clause.impl.BooleanQueryClauseBuilder;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ClauseBuilder;

import com.google.gson.JsonObject;

class MultiFieldQueryClauseCommonState<N, F extends MultiFieldQueryClauseCommonState.FieldSetContext>
		implements ClauseBuilder<JsonObject> {

	private final QueryTargetContext targetContext;

	private final Supplier<N> nextContextProvider;

	private final List<F> fieldSetContexts = new ArrayList<>();

	public MultiFieldQueryClauseCommonState(QueryTargetContext targetContext, Supplier<N> nextContextProvider) {
		this.targetContext = targetContext;
		this.nextContextProvider = nextContextProvider;
	}

	public QueryTargetContext getTargetContext() {
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
	public JsonObject build() {
		List<JsonObject> queries = new ArrayList<>();
		for ( F fieldSetContext : fieldSetContexts ) {
			fieldSetContext.contribute( queries::add );
		}
		if ( queries.size() > 1 ) {
			BooleanQueryClauseBuilder boolBuilder = targetContext.getClauseFactory().bool();
			queries.stream().forEach( boolBuilder::should );
			return boolBuilder.build();
		}
		else {
			return queries.get( 0 );
		}
	}

	public interface FieldSetContext {
		void contribute(Consumer<JsonObject> collector);
	}
}