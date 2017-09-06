/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.Arrays;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.search.clause.impl.ClauseBuilder;
import org.hibernate.search.engine.search.dsl.RangeClauseContext;
import org.hibernate.search.engine.search.dsl.RangeClauseFieldSetContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class RangeClauseContextImpl<N> implements RangeClauseContext<N>, ClauseBuilder<JsonObject> {

	private final RangeClauseFieldSetContextImpl.CommonState<N> commonState;

	public RangeClauseContextImpl(QueryTargetContext targetContext, Supplier<N> nextContextProvider) {
		this.commonState = new RangeClauseFieldSetContextImpl.CommonState<N>( targetContext, nextContextProvider );
	}

	@Override
	public RangeClauseFieldSetContext<N> onFields(String ... fieldName) {
		return new RangeClauseFieldSetContextImpl<>( commonState, Arrays.asList( fieldName ) );
	}

	@Override
	public JsonObject build() {
		return commonState.build();
	}

}
