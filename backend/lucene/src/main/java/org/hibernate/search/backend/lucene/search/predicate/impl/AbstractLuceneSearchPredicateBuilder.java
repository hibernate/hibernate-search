/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.List;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;


public abstract class AbstractLuceneSearchPredicateBuilder implements SearchPredicateBuilder<LuceneSearchPredicateBuilder>,
		LuceneSearchPredicateBuilder {

	private Float boost;
	private boolean constantScore;

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public void constantScore() {
		this.constantScore = true;
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public Query build(LuceneSearchPredicateContext context) {
		Query query = doBuild( context );

		// the boost should be applied on top of the constant score,
		// otherwise the boost will simply be ignored
		if ( constantScore ) {
			query = new ConstantScoreQuery( query );
		}
		if ( boost != null ) {
			query = new BoostQuery( query, boost );
		}

		return query;
	}

	protected abstract Query doBuild(LuceneSearchPredicateContext context);

	protected Query applyImplicitNestedSteps(List<String> nestedPathHierarchy, LuceneSearchPredicateContext context, Query baseQuery) {
		Query result = baseQuery;

		// traversing the furtherImplicitNestedSteps in the inverted order
		for ( int i = 0; i < nestedPathHierarchy.size(); i++ ) {
			int index = nestedPathHierarchy.size() - 1 - i;
			String path = nestedPathHierarchy.get( index );
			if ( path.equals( context.getNestedPath() ) ) {
				// the upper levels have been handled by the explicit predicate/s
				break;
			}

			LuceneSearchPredicateContext childContext = ( index == 0 ) ? context : new LuceneSearchPredicateContext( nestedPathHierarchy.get( index - 1 ) );
			result = LuceneNestedPredicateBuilder.doBuild( childContext, path, result );
		}

		return result;
	}
}
