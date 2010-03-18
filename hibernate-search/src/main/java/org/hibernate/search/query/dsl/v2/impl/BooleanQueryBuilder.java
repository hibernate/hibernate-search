package org.hibernate.search.query.dsl.v2.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.query.dsl.v2.BooleanJunction;
import org.hibernate.search.query.dsl.v2.MustJunction;

/**
 * @author Emmanuel Bernard
 */
class BooleanQueryBuilder implements MustJunction {
	private final List<BooleanClause> clauses;
	private final QueryCustomizer queryCustomizer;

	BooleanQueryBuilder() {
		clauses = new ArrayList<BooleanClause>(5);
		queryCustomizer = new QueryCustomizer();
	}
	
	public BooleanJunction not() {
		final int lastIndex = clauses.size() -1;
		final BooleanClause last = clauses.get(lastIndex);
		if ( ! last.getOccur().equals( BooleanClause.Occur.MUST ) ) {
			throw new AssertionFailure( "Cannot negate class: " + last.getOccur() );
		}
		clauses.add( lastIndex, new BooleanClause( last.getQuery(), BooleanClause.Occur.MUST_NOT ) );
		return this;
	}

	public BooleanJunction should(Query query) {
		clauses.add( new BooleanClause( query, BooleanClause.Occur.SHOULD ) );
		return this;
	}

	public MustJunction must(Query query) {
		clauses.add( new BooleanClause( query, BooleanClause.Occur.SHOULD ) );
		return this;
	}

	public MustJunction boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public MustJunction constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public MustJunction filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}

	public Query createQuery() {
		final int nbrOfClauses = clauses.size();
		if ( nbrOfClauses == 0) {
			throw new AssertionFailure( "Cannot create an empty boolean query" );
		}
		else if ( nbrOfClauses == 1 ) {
			final BooleanClause uniqueClause = clauses.get( 0 );
			if ( uniqueClause.getOccur().equals( BooleanClause.Occur.MUST_NOT ) ) {
				//FIXME We have two choices here, raise an exception or combine with an All query. #2 is done atm.
				//TODO which normfield to use and how to pass it?
				should( new MatchAllDocsQuery() );
			}
			else {
				//optimize
				return queryCustomizer.setWrappedQuery( uniqueClause.getQuery() ).createQuery();
			}
		}

		BooleanQuery query = new BooleanQuery( );
		for (BooleanClause clause : clauses) {
			query.add( clause );
		}
		return queryCustomizer.setWrappedQuery( query ).createQuery();
	}
}
