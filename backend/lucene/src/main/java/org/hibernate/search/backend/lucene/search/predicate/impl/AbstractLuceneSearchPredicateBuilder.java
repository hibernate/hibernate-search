/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;


public abstract class AbstractLuceneSearchPredicateBuilder implements SearchPredicateBuilder,
		LuceneSearchPredicateBuilder {

	protected final LuceneSearchContext searchContext;

	private Float boost;
	private boolean constantScore;

	protected AbstractLuceneSearchPredicateBuilder(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
	}

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public void constantScore() {
		this.constantScore = true;
	}

	@Override
	public SearchPredicate build() {
		// TODO HSEARCH-3476 this is just a temporary hack:
		//  we should move to one SearchPredicate implementation per type of predicate.
		return LuceneSearchPredicate.of( searchContext, this );
	}

	@Override
	public Query toQuery(PredicateRequestContext context) {
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

	protected abstract Query doBuild(PredicateRequestContext context);

}
