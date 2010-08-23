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
