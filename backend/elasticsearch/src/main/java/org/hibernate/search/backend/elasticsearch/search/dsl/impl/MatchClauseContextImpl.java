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
import org.hibernate.search.engine.search.dsl.MatchClauseContext;
import org.hibernate.search.engine.search.dsl.MatchClauseFieldSetContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class MatchClauseContextImpl<N> implements MatchClauseContext<N>, ClauseBuilder<JsonObject> {

	private final MatchClauseFieldSetContextImpl.CommonState<N> commonState;

	public MatchClauseContextImpl(QueryTargetContext targetContext, Supplier<N> nextContextProvider) {
		this.commonState = new MatchClauseFieldSetContextImpl.CommonState<N>( targetContext, nextContextProvider );
	}

	@Override
	public MatchClauseFieldSetContext<N> onFields(String ... fieldName) {
		return new MatchClauseFieldSetContextImpl<>( commonState, Arrays.asList( fieldName ) );
	}

	@Override
	public JsonObject build() {
		return commonState.build();
	}

}
