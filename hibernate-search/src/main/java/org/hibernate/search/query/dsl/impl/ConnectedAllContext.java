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

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.search.query.dsl.AllContext;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedAllContext implements AllContext {
	private final List<BooleanClause> clauses;
	private final QueryCustomizer queryCustomizer;

	public ConnectedAllContext() {
		this.queryCustomizer = new QueryCustomizer();
		this.clauses = new ArrayList<BooleanClause>(5);
		this.clauses.add( new BooleanClause( new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD ) );
	}

	public Query createQuery() {
		Query query;
		if ( clauses.size() == 1 ) {
			query = clauses.get( 0 ).getQuery();
		}
		else {
			BooleanQuery booleanQuery = new BooleanQuery( );
			for (BooleanClause clause : clauses) {
				booleanQuery.add( clause );
			}
			query = booleanQuery;
		}
		return queryCustomizer.setWrappedQuery( query ).createQuery();
	}

	public AllContext except(Query... queriesMatchingExcludedDocuments) {
		for (Query query : queriesMatchingExcludedDocuments) {
			clauses.add( new BooleanClause( query, BooleanClause.Occur.MUST_NOT ) );
		}
		return this;
	}

	public AllContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public AllContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	public AllContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
