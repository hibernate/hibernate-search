/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.spi.LuceneMigrationUtils;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.MustJunction;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class BooleanQueryBuilder implements MustJunction {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final QueryBuildingContext queryContext;
	private final List<BooleanClause> clauses;
	private BooleanClause lastClause;
	private final QueryCustomizer queryCustomizer;
	private MinimumShouldMatchContextImpl minimumShouldMatchContext;

	BooleanQueryBuilder(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		clauses = new ArrayList<>( 5 );
		queryCustomizer = new QueryCustomizer();
	}

	@Override
	public BooleanJunction not() {
		replaceLastMustWith( Occur.MUST_NOT );
		return this;
	}

	@Override
	public BooleanJunction disableScoring() {
		replaceLastMustWith( Occur.FILTER );
		return this;
	}

	private void replaceLastMustWith(Occur replacementOccur) {
		if ( lastClause == null ) {
			return;
		}
		if ( !lastClause.getOccur().equals( Occur.MUST ) ) {
			throw new AssertionFailure( "Cannot negate or disable scoring on class: " + lastClause.getOccur() );
		}
		final int lastIndex = clauses.size() - 1;
		clauses.set( lastIndex, new BooleanClause( lastClause.getQuery(), replacementOccur ) );
	}

	@Override
	public BooleanJunction should(Query query) {
		if ( query == null ) {
			lastClause = null;
		}
		else {
			lastClause = new BooleanClause( query, Occur.SHOULD );
			clauses.add( lastClause );
		}
		return this;
	}

	@Override
	public MustJunction must(Query query) {
		if ( query == null ) {
			lastClause = null;
		}
		else {
			lastClause = new BooleanClause( query, Occur.MUST );
			clauses.add( lastClause );
		}
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
	public MustJunction filteredBy(Query filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}

	@Override
	public Query createQuery() {
		return LuceneMigrationUtils.toLuceneQuery( createPredicate() );
	}

	private SearchPredicate createPredicate() {
		final int nbrOfClauses = clauses.size();
		if ( nbrOfClauses == 0 ) {
			throw log.booleanQueryWithoutClauses();
		}

		SearchPredicateFactory factory = queryContext.getScope().predicate();
		BooleanPredicateClausesStep<?> step = factory.bool();
		for ( BooleanClause clause : clauses ) {
			SearchPredicate predicate = factory.extension( LuceneExtension.get() )
					.fromLuceneQuery( clause.getQuery() ).toPredicate();
			switch ( clause.getOccur() ) {
				case MUST:
					step = step.must( predicate );
					break;
				case FILTER:
					step = step.filter( predicate );
					break;
				case SHOULD:
					step = step.should( predicate );
					break;
				case MUST_NOT:
					step = step.mustNot( predicate );
					break;
			}
		}

		if ( minimumShouldMatchContext != null ) {
			minimumShouldMatchContext.applyMinimum( step );
		}

		queryCustomizer.applyScoreOptions( step );
		queryCustomizer.applyFilter( factory, step );

		return step.toPredicate();
	}

	@Override
	public boolean isEmpty() {
		return clauses.isEmpty();
	}

	@Override
	public BooleanJunction minimumShouldMatchNumber(int matchingClausesNumber) {
		getMinimumShouldMatchContext().requireNumber( matchingClausesNumber );
		return this;
	}

	@Override
	public BooleanJunction minimumShouldMatchPercent(int matchingClausesPercent) {
		getMinimumShouldMatchContext().requirePercent( matchingClausesPercent );
		return this;
	}

	private MinimumShouldMatchContextImpl getMinimumShouldMatchContext() {
		if ( minimumShouldMatchContext == null ) {
			minimumShouldMatchContext = new MinimumShouldMatchContextImpl();
		}
		return minimumShouldMatchContext;
	}

}
