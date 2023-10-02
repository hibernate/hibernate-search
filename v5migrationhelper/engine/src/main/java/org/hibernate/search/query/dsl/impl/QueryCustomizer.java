/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.PredicateScoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.query.dsl.QueryCustomization;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class QueryCustomizer implements QueryCustomization<QueryCustomizer> {
	private float boost = 1f;
	private boolean constantScore;
	private Query filter;

	@Override
	public QueryCustomizer boostedTo(float boost) {
		this.boost = boost * this.boost;
		return this;
	}

	@Override
	public QueryCustomizer withConstantScore() {
		constantScore = true;
		return this;
	}

	@Override
	public QueryCustomizer filteredBy(Query filter) {
		this.filter = filter;
		return this;
	}

	// TODO: this is ugly: we probably need to rethink how this is built to not depend on Lucene behavior
	public float getBoost() {
		return boost;
	}

	public void applyScoreOptions(PredicateScoreStep<?> step) {
		if ( boost != 1.0f ) {
			step.boost( boost );
		}
		if ( constantScore ) {
			step.constantScore();
		}
	}

	public SearchPredicate applyFilter(SearchPredicateFactory factory, SearchPredicate predicate) {
		if ( filter == null ) {
			return predicate;
		}
		BooleanPredicateClausesStep<?> step = factory.bool().must( predicate );
		applyFilter( factory, step );
		return step.toPredicate();
	}

	public void applyFilter(SearchPredicateFactory factory, BooleanPredicateOptionsCollector<?> collector) {
		if ( filter == null ) {
			return;
		}
		collector.filter( factory.extension( LuceneExtension.get() ).fromLuceneQuery( filter ) );
	}
}
