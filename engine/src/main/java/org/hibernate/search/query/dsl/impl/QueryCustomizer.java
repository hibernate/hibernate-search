/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.dsl.QueryCustomization;

/**
 * @author Emmanuel Bernard
 */
class QueryCustomizer implements QueryCustomization<QueryCustomizer> {
	private float boost = 1f;
	private boolean constantScore;
	private Query wrappedQuery;
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

	public QueryCustomizer setWrappedQuery(Query wrappedQuery) {
		this.wrappedQuery = wrappedQuery;
		return this;
	}

	// TODO: this is ugly: we probably need to rethink how this is built to not depend on Lucene behavior
	public float getBoost() {
		return boost;
	}

	public Query createQuery() {
		Query finalQuery = wrappedQuery;
		if ( wrappedQuery == null ) {
			throw new AssertionFailure( "wrapped query not set" );
		}
		finalQuery.setBoost( boost * finalQuery.getBoost() );
		if ( filter != null ) {
			finalQuery = new BooleanQuery.Builder()
					.add( finalQuery, Occur.MUST )
					.add( filter, Occur.FILTER )
					.build();
		}
		if ( constantScore ) {
			finalQuery = new ConstantScoreQuery( finalQuery );
		}
		return finalQuery;
	}
}
