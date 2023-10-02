/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.WithParametersPredicateBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchWithParametersPredicate extends AbstractElasticsearchPredicate {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator;

	private ElasticsearchWithParametersPredicate(Builder builder) {
		super( builder );
		this.scope = builder.scope;
		this.predicateCreator = builder.predicateCreator;
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
		ElasticsearchSearchPredicate providedPredicate =
				ElasticsearchSearchPredicate.from( scope, predicateCreator.apply( context.queryParameters() ).toPredicate() );

		providedPredicate.checkNestableWithin( PredicateNestingContext.nested( context.getNestedPath() ) );

		return providedPredicate.toJsonQuery( context );
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		// do nothing; We'll check it in to-query method.
	}

	public static class Builder extends AbstractBuilder implements WithParametersPredicateBuilder {
		private Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void creator(Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator) {
			this.predicateCreator = predicateCreator;
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchWithParametersPredicate( this );
		}
	}
}
