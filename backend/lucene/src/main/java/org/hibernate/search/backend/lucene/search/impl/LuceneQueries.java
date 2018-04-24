/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

public class LuceneQueries {

	private static final Query MAIN_DOCUMENT_QUERY = new TermQuery( new Term( LuceneFields.typeFieldName(), LuceneFields.TYPE_MAIN_DOCUMENT ) );

	private static final Query CHILD_DOCUMENT_QUERY = new TermQuery( new Term( LuceneFields.typeFieldName(), LuceneFields.TYPE_CHILD_DOCUMENT ) );

	private LuceneQueries() {
	}

	public static Query mainDocumentQuery() {
		return MAIN_DOCUMENT_QUERY;
	}

	public static Query childDocumentQuery() {
		return CHILD_DOCUMENT_QUERY;
	}

	public static Query nestedDocumentPathQuery(String absoluteFieldPath) {
		return new TermQuery( new Term( LuceneFields.nestedDocumentPathFieldName(), absoluteFieldPath ) );
	}

	public static Query discriminatorMultiTenancyDeleteDocumentQuery(String tenantId, String id) {
		return wrapWithDiscriminatorTenantIdQuery( new TermQuery( new Term( LuceneFields.idFieldName(), id ) ), tenantId );
	}

	public static Query wrapWithDiscriminatorTenantIdQuery(Query originalLuceneQuery, String tenantId) {
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		queryBuilder.add( originalLuceneQuery, Occur.MUST );
		queryBuilder.add( new TermQuery( new Term( LuceneFields.tenantIdFieldName(), tenantId ) ), Occur.FILTER );

		return queryBuilder.build();
	}
}
