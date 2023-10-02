/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.spi.LuceneMigrationUtils;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchAllPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.query.dsl.AllContext;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedAllContext implements AllContext {
	private final QueryBuildingContext queryContext;
	private final List<Query> except;
	private final QueryCustomizer queryCustomizer;

	public ConnectedAllContext(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = new QueryCustomizer();
		this.except = new ArrayList<>();
	}

	@Override
	public Query createQuery() {
		return LuceneMigrationUtils.toLuceneQuery( createPredicate() );
	}

	private SearchPredicate createPredicate() {
		SearchPredicateFactory factory = queryContext.getScope().predicate();

		MatchAllPredicateOptionsStep<?> optionsStep = factory.matchAll();

		for ( Query query : except ) {
			optionsStep = optionsStep.except( factory.extension( LuceneExtension.get() ).fromLuceneQuery( query ) );
		}

		queryCustomizer.applyScoreOptions( optionsStep );
		SearchPredicate predicate = optionsStep.toPredicate();
		return queryCustomizer.applyFilter( factory, predicate );
	}

	@Override
	public AllContext except(Query... queriesMatchingExcludedDocuments) {
		Collections.addAll( except, queriesMatchingExcludedDocuments );
		return this;
	}

	@Override
	public AllContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public AllContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public AllContext filteredBy(Query filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
