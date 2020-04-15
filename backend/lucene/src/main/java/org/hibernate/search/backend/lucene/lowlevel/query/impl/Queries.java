/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.join.BitSetProducer;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ToChildBlockJoinQuery;

public class Queries {

	private static final Query MAIN_DOCUMENT_QUERY = new TermQuery( new Term( MetadataFields.typeFieldName(), MetadataFields.TYPE_MAIN_DOCUMENT ) );

	private static final Query CHILD_DOCUMENT_QUERY = new TermQuery( new Term( MetadataFields.typeFieldName(), MetadataFields.TYPE_CHILD_DOCUMENT ) );

	private Queries() {
	}

	public static Query mainDocumentQuery() {
		return MAIN_DOCUMENT_QUERY;
	}

	public static Query childDocumentQuery() {
		return CHILD_DOCUMENT_QUERY;
	}

	public static Query nestedDocumentPathQuery(String absoluteFieldPath) {
		return new TermQuery( new Term( MetadataFields.nestedDocumentPathFieldName(), absoluteFieldPath ) );
	}

	public static Query boolFilter(Query must, Query filter) {
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		queryBuilder.add( must, Occur.MUST );
		queryBuilder.add( filter, Occur.FILTER );
		return queryBuilder.build();
	}

	public static Query boolFilter(Query must, List<Query> filters) {
		if ( filters == null || filters.isEmpty() ) {
			return must;
		}

		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		queryBuilder.add( must, Occur.MUST );
		for ( Query filter : filters ) {
			queryBuilder.add( filter, Occur.FILTER );
		}
		return queryBuilder.build();
	}

	public static Query term(String absoluteFieldPath, String value) {
		return new TermQuery( new Term( absoluteFieldPath, value ) );
	}

	public static Query anyTerm(String absoluteFieldPath, Set<String> values) {
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		for ( String routingKey : values ) {
			queryBuilder.add(
					new TermQuery( new Term( absoluteFieldPath, routingKey ) ),
					Occur.SHOULD
			);
		}
		return queryBuilder.build();
	}

	public static BitSetProducer parentFilter(String parentNestedDocumentPath) {
		Query parentQuery;
		if ( parentNestedDocumentPath == null ) {
			parentQuery = Queries.mainDocumentQuery();
		}
		else {
			parentQuery = Queries.nestedDocumentPathQuery( parentNestedDocumentPath );
		}
		return new QueryBitSetProducer( parentQuery );
	}

	public static BooleanQuery findChildQuery(BitSetProducer parentFilter,
				Set<String> nestedDocumentPaths, Query originalParentQuery,
				Query nestedFilter) {
		ToChildBlockJoinQuery parentQuery = new ToChildBlockJoinQuery( originalParentQuery, parentFilter );

		BooleanQuery.Builder builder = new BooleanQuery.Builder();

		builder.add( parentQuery, Occur.MUST )
				.add( createNestedDocumentPathSubQuery( nestedDocumentPaths ), Occur.FILTER )
				.add( childDocumentQuery(), Occur.FILTER );

		if ( nestedFilter != null ) {
			builder.add( nestedFilter, Occur.FILTER );
		}

		return builder.build();
	}

	private static BooleanQuery createNestedDocumentPathSubQuery(Set<String> nestedDocumentPaths) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( String nestedDocumentPath : nestedDocumentPaths ) {
			builder.add( nestedDocumentPathQuery( nestedDocumentPath ), Occur.SHOULD );
		}
		builder.setMinimumNumberShouldMatch( 1 );
		return builder.build();
	}
}
