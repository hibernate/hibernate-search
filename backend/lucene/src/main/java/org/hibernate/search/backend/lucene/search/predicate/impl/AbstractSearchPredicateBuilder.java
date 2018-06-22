/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;


/**
 * @author Guillaume Smet
 */
abstract class AbstractSearchPredicateBuilder implements SearchPredicateBuilder<LuceneSearchPredicateContext, LuceneSearchPredicateCollector> {

	private Float boost;

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	protected abstract Query buildQuery(LuceneSearchPredicateContext context);

	@Override
	public void contribute(LuceneSearchPredicateContext context, LuceneSearchPredicateCollector collector) {
		if ( boost != null ) {
			collector.collectPredicate( new BoostQuery( buildQuery( context ), boost ) );
		}
		else {
			collector.collectPredicate( buildQuery( context ) );
		}
	}

	protected Query getQueryFromContributor(LuceneSearchPredicateContext context,
			SearchPredicateContributor<LuceneSearchPredicateContext, ? super LuceneSearchPredicateCollector> contributor) {
		LuceneSearchPredicateQueryBuilder queryBuilder = new LuceneSearchPredicateQueryBuilder();
		contributor.contribute( context, queryBuilder );
		return queryBuilder.build();
	}
}
