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

import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class MatchPredicateContextImpl<N> implements MatchPredicateContext<N>, ElasticsearchSearchPredicateContributor {

	private final MatchPredicateFieldSetContextImpl.CommonState<N> commonState;

	public MatchPredicateContextImpl(SearchTargetContext targetContext, Supplier<N> nextContextProvider) {
		this.commonState = new MatchPredicateFieldSetContextImpl.CommonState<N>( targetContext, nextContextProvider );
	}

	@Override
	public MatchPredicateFieldSetContext<N> onFields(String ... fieldName) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( fieldName ) );
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		commonState.contribute( collector );
	}

}
