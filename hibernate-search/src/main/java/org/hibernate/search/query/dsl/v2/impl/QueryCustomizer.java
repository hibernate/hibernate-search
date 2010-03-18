package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryFilter;
import org.apache.lucene.search.QueryWrapperFilter;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.query.dsl.v2.QueryCustomization;

/**
 * @author Emmanuel Bernard
 */
class QueryCustomizer implements QueryCustomization<QueryCustomizer> {
	private float boost = 1f;
	private boolean constantScore;
	private Query wrappedQuery;
	private Filter filter;

	public QueryCustomizer boostedTo(float boost) {
		this.boost = boost;
		return this;
	}

	public QueryCustomizer constantScore() {
		constantScore = true;
		return this;
	}

	public QueryCustomizer filter(Filter filter) {
		this.filter = filter;
		return this;
	}

	public QueryCustomizer setWrappedQuery(Query wrappedQuery) {
		this.wrappedQuery = wrappedQuery;
		return this;
	}

	public Query createQuery() {
		Query finalQuery = wrappedQuery;
		if (wrappedQuery == null) {
			throw new AssertionFailure( "wrapped query not set" );
		}
		finalQuery.setBoost( boost );
		if (filter != null) {
			finalQuery = new FilteredQuery(finalQuery, filter);
		}
		if ( constantScore ) {
			finalQuery = new ConstantScoreQuery( new QueryWrapperFilter( finalQuery ) );
		}
		return finalQuery;
	}
}
