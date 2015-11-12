/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.MustJunction;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
class BooleanQueryBuilder implements MustJunction {

	private static final Log log = LoggerFactory.make();

	private final List<BooleanClause> clauses;
	private final QueryCustomizer queryCustomizer;

	BooleanQueryBuilder() {
		clauses = new ArrayList<BooleanClause>( 5 );
		queryCustomizer = new QueryCustomizer();
	}

	@Override
	public BooleanJunction not() {
		replaceLastMustWith( BooleanClause.Occur.MUST_NOT );
		return this;
	}

	@Override
	public BooleanJunction disableScoring() {
		replaceLastMustWith( BooleanClause.Occur.FILTER );
		return this;
	}

	private void replaceLastMustWith(Occur replacementOccur) {
		final int lastIndex = clauses.size() - 1;
		final BooleanClause last = clauses.get( lastIndex );
		if ( ! last.getOccur().equals( BooleanClause.Occur.MUST ) ) {
			throw new AssertionFailure( "Cannot negate or disable scoring on class: " + last.getOccur() );
		}
		clauses.set( lastIndex, new BooleanClause( last.getQuery(), replacementOccur ) );
	}

	@Override
	public BooleanJunction should(Query query) {
		clauses.add( new BooleanClause( query, BooleanClause.Occur.SHOULD ) );
		return this;
	}

	@Override
	public MustJunction must(Query query) {
		clauses.add( new BooleanClause( query, BooleanClause.Occur.MUST ) );
		return this;
	}

	@Override
	public MustJunction boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public MustJunction withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public MustJunction filteredBy(Filter filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}

	@Override
	public Query createQuery() {
		final int nbrOfClauses = clauses.size();
		if ( nbrOfClauses == 0 ) {
			throw log.booleanQueryWithoutClauses();
		}
		else if ( nbrOfClauses == 1 ) {
			final BooleanClause uniqueClause = clauses.get( 0 );
			final Occur occur = uniqueClause.getOccur();
			if ( occur == Occur.FILTER || occur == Occur.MUST_NOT ) {
				//In the case it's a FILTER or MUST_NOT clause,
				//and it's a unique clause, we need to pair it up with a match-all query
				should( new MatchAllDocsQuery() );
			}
			else {
				//optimize
				return queryCustomizer.setWrappedQuery( uniqueClause.getQuery() ).createQuery();
			}
		}

		Builder builder = new org.apache.lucene.search.BooleanQuery.Builder();
		for ( BooleanClause clause : clauses ) {
			builder.add( clause );
		}
		return queryCustomizer.setWrappedQuery( builder.build() ).createQuery();
	}

	@Override
	public boolean isEmpty() {
		return clauses.isEmpty();
	}

}
