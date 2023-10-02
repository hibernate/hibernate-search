/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.backend.lucene.search.spi.LuceneMigrationUtils;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateScoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.query.dsl.Termination;

import org.apache.lucene.search.Query;

abstract class AbstractConnectedMultiFieldsQueryBuilder<T, F extends PredicateScoreStep<? extends F> & PredicateFinalStep>
		implements Termination<T> {

	protected final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final FieldsContext fieldsContext;

	public AbstractConnectedMultiFieldsQueryBuilder(QueryBuildingContext queryContext,
			QueryCustomizer queryCustomizer, FieldsContext fieldsContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContext = fieldsContext;
	}

	@Override
	public final Query createQuery() {
		return LuceneMigrationUtils.toLuceneQuery( createPredicate() );
	}

	private SearchPredicate createPredicate() {
		SearchPredicateFactory factory = queryContext.getScope().predicate();
		if ( fieldsContext.size() == 1 ) {
			F finalStep = createPredicate( factory, fieldsContext.getFirst() );
			queryCustomizer.applyScoreOptions( finalStep );
			SearchPredicate predicate = finalStep.toPredicate();
			return queryCustomizer.applyFilter( factory, predicate );
		}
		else {
			BooleanPredicateClausesStep<?> boolStep = factory.bool().with( b -> {
				for ( FieldContext fieldContext : fieldsContext ) {
					b.should( createPredicate( factory, fieldContext ) );
				}
				queryCustomizer.applyFilter( factory, b );
			} );
			queryCustomizer.applyScoreOptions( boolStep );
			return boolStep.toPredicate();
		}
	}

	protected abstract F createPredicate(SearchPredicateFactory factory, FieldContext fieldContext);
}
