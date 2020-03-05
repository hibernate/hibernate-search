/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.ESToParentBlockJoinQuery;

class LuceneNestedPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
	implements NestedPredicateBuilder<LuceneSearchPredicateBuilder> {

	private final String absoluteFieldPath;

	private LuceneSearchPredicateBuilder nestedBuilder;

	LuceneNestedPredicateBuilder(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public void nested(LuceneSearchPredicateBuilder nestedBuilder) {
		this.nestedBuilder = nestedBuilder;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		LuceneSearchPredicateContext childContext = new LuceneSearchPredicateContext( absoluteFieldPath );

		BooleanQuery.Builder childQueryBuilder = new BooleanQuery.Builder();
		childQueryBuilder.add( Queries.childDocumentQuery(), Occur.FILTER );
		childQueryBuilder.add( Queries.nestedDocumentPathQuery( absoluteFieldPath ), Occur.FILTER );
		childQueryBuilder.add( nestedBuilder.build( childContext ), Occur.MUST );

		Query parentQuery;
		if ( context.getNestedPath() == null ) {
			parentQuery = Queries.mainDocumentQuery();
		}
		else {
			parentQuery = Queries.nestedDocumentPathQuery( context.getNestedPath() );
		}

		return new ESToParentBlockJoinQuery( childQueryBuilder.build(), new QueryBitSetProducer( parentQuery ), ScoreMode.Avg, absoluteFieldPath );
	}
}
