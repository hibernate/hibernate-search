/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.dsl.QueryCustomization;

/**
 * @author Emmanuel Bernard
 */
class QueryCustomizer implements QueryCustomization<QueryCustomizer> {
	private float boost = 1f;
	private boolean constantScore;
	private Query wrappedQuery;
	private Filter filter;

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
	public QueryCustomizer filteredBy(Filter filter) {
		this.filter = filter;
		return this;
	}

	public QueryCustomizer setWrappedQuery(Query wrappedQuery) {
		this.wrappedQuery = wrappedQuery;
		return this;
	}

	public Query createQuery() {
		Query finalQuery = wrappedQuery;
		if ( wrappedQuery == null ) {
			throw new AssertionFailure( "wrapped query not set" );
		}
		finalQuery.setBoost( boost * finalQuery.getBoost() );
		if ( filter != null ) {
			finalQuery = new FilteredQuery(finalQuery, filter);
		}
		if ( constantScore ) {
			finalQuery = new ConstantScoreQuery( new QueryWrapperFilter( finalQuery ) );
		}
		return finalQuery;
	}
}
