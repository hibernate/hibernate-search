/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateFieldSetContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class RangePredicateContextImpl<N> implements RangePredicateContext<N>, ElasticsearchSearchPredicateContributor {

	private final RangePredicateFieldSetContextImpl.CommonState<N> commonState;

	public RangePredicateContextImpl(SearchTargetContext targetContext, Supplier<N> nextContextProvider) {
		this.commonState = new RangePredicateFieldSetContextImpl.CommonState<N>( targetContext, nextContextProvider );
	}

	@Override
	public RangePredicateFieldSetContext<N> onFields(String ... fieldName) {
		return new RangePredicateFieldSetContextImpl<>( commonState, Arrays.asList( fieldName ) );
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		commonState.contribute( collector );
	}
}
