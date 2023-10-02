/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.WithParametersPredicateBuilder;

import org.apache.lucene.search.Query;

public class LuceneWithParametersPredicate extends AbstractLuceneSearchPredicate {

	private final LuceneSearchIndexScope<?> scope;
	private final Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator;

	private LuceneWithParametersPredicate(Builder builder) {
		super( builder );
		scope = builder.scope;
		predicateCreator = builder.predicateCreator;
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		LuceneSearchPredicate providedPredicate =
				LuceneSearchPredicate.from( scope, predicateCreator.apply( context.queryParameters() ).toPredicate() );
		providedPredicate.checkNestableWithin( context.getNestedPath() );

		return providedPredicate.toQuery( context );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// do nothing; We'll check it in to-query method.
	}

	public static class Builder extends AbstractBuilder implements WithParametersPredicateBuilder {
		private Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator;

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void creator(Function<? super NamedValues, ? extends PredicateFinalStep> creator) {
			this.predicateCreator = creator;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneWithParametersPredicate( this );
		}
	}
}
