/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.query.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
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

	public static Query singleDocumentQuery(String tenantId, String id) {
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		queryBuilder.add( new TermQuery( new Term( MetadataFields.idFieldName(), id ) ), Occur.MUST );
		queryBuilder.add( tenantIdQuery( tenantId ), Occur.FILTER );
		return queryBuilder.build();
	}

	public static Query tenantIdQuery(String tenantId) {
		return new TermQuery( new Term( MetadataFields.tenantIdFieldName(), tenantId ) );
	}

	public static BooleanQuery findChildQuery(Set<String> nestedDocumentPaths, Query originalParentQuery) {
		QueryBitSetProducer parentsFilter = new QueryBitSetProducer( mainDocumentQuery() );
		return findChildQuery( nestedDocumentPaths, originalParentQuery, parentsFilter );
	}

	public static BooleanQuery findChildQuery(Set<String> nestedDocumentPaths, Query originalParentQuery,
			QueryBitSetProducer parentsFilter) {
		ToChildBlockJoinQuery parentQuery = new ToChildBlockJoinQuery( originalParentQuery, parentsFilter );

		return new BooleanQuery.Builder()
				.add( parentQuery, Occur.MUST )
				.add( createNestedDocumentPathSubQuery( nestedDocumentPaths ), Occur.FILTER )
				.add( childDocumentQuery(), Occur.FILTER )
				.build();
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
