/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.WithParametersPredicateBuilder;

import org.apache.lucene.search.Query;

public class LuceneWithParametersPredicate extends AbstractLuceneSingleFieldPredicate {

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

	public static class Factory extends AbstractLuceneCompositeNodeSearchQueryElementFactory<WithParametersPredicateBuilder> {

		public static final Factory INSTANCE = new Factory();

		@Override
		public WithParametersPredicateBuilder create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			return new Builder( scope, node );
		}
	}

	private static class Builder extends AbstractBuilder implements WithParametersPredicateBuilder {
		private Function<? super NamedValues, ? extends PredicateFinalStep> predicateCreator;

		Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext node) {
			super( scope, node );
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
