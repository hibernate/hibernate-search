/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

public class Queries {

	private static final Query MAIN_DOCUMENT_QUERY =
			new TermQuery( new Term( MetadataFields.typeFieldName(), MetadataFields.TYPE_MAIN_DOCUMENT ) );

	private static final Query CHILD_DOCUMENT_QUERY =
			new TermQuery( new Term( MetadataFields.typeFieldName(), MetadataFields.TYPE_CHILD_DOCUMENT ) );

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

	public static Query parentsFilterQuery(String parentNestedDocumentPath) {
		if ( parentNestedDocumentPath == null ) {
			return Queries.mainDocumentQuery();
		}
		else {
			return Queries.nestedDocumentPathQuery( parentNestedDocumentPath );
		}
	}

	// Users of this query will target a specific parent ID which is guaranteed to already match the main Lucene query,
	// so we don't need to filter parent documents nor to use a ToChildBlockJoinQuery.
	public static BooleanQuery childDocumentsQuery(Set<String> nestedDocumentPaths, Query nestedFilter) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( String nestedDocumentPath : nestedDocumentPaths ) {
			builder.add( nestedDocumentPathQuery( nestedDocumentPath ), Occur.SHOULD );
		}
		if ( nestedFilter != null ) {
			builder.add( nestedFilter, Occur.FILTER );
		}
		builder.setMinimumNumberShouldMatch( 1 );
		return builder.build();
	}
}
