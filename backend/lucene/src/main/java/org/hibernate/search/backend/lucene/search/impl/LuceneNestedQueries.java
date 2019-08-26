/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.Set;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ToChildBlockJoinQuery;

public class LuceneNestedQueries {

	private LuceneNestedQueries() {
	}

	public static BooleanQuery findChildQuery(Set<String> nestedDocumentPaths, Query originalParentQuery) {
		QueryBitSetProducer parentsFilter = new QueryBitSetProducer( LuceneQueries.mainDocumentQuery() );
		ToChildBlockJoinQuery parentQuery = new ToChildBlockJoinQuery( originalParentQuery, parentsFilter );

		return new BooleanQuery.Builder()
				.add( parentQuery, BooleanClause.Occur.MUST )
				.add( createNestedDocumentPathSubQuery( nestedDocumentPaths ), BooleanClause.Occur.FILTER )
				.add( LuceneQueries.childDocumentQuery(), BooleanClause.Occur.FILTER )
				.build();
	}

	private static BooleanQuery createNestedDocumentPathSubQuery(Set<String> nestedDocumentPaths) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( String nestedDocumentPath : nestedDocumentPaths ) {
			builder.add( LuceneQueries.nestedDocumentPathQuery( nestedDocumentPath ), BooleanClause.Occur.SHOULD );
		}
		builder.setMinimumNumberShouldMatch( 1 );
		return builder.build();
	}
}
