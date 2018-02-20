/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;


/**
 * @author Guillaume Smet
 */
class NestedPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements NestedPredicateBuilder<LuceneSearchPredicateCollector> {

	private final String absoluteFieldPath;

	private Query nestedQuery;

	NestedPredicateBuilderImpl(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public LuceneSearchPredicateCollector getNestedCollector() {
		return this::nested;
	}

	private void nested(Query query) {
		this.nestedQuery = query;
	}

	@Override
	protected Query buildQuery() {
		BooleanQuery.Builder childQueryBuilder = new BooleanQuery.Builder();
		childQueryBuilder.add( LuceneQueries.childDocumentQuery(), Occur.FILTER );
		childQueryBuilder.add( LuceneQueries.nestedDocumentPathQuery( absoluteFieldPath ), Occur.FILTER );
		childQueryBuilder.add( nestedQuery, Occur.MUST );

		// TODO at some point we should have a parameter for the score mode
		return new ToParentBlockJoinQuery( childQueryBuilder.build(), new QueryBitSetProducer( LuceneQueries.mainDocumentQuery() ), ScoreMode.Avg );
	}
}
