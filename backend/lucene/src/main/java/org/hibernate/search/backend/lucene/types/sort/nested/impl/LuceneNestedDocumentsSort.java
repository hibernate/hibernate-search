/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.nested.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.impl.LuceneNestedQueries;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneChildrenCollector;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

public class LuceneNestedDocumentsSort {

	private static final Set<String> ID_FIELD_SET = Collections.singleton( LuceneFields.idFieldName() );

	private final Map<String, Set<LuceneNestedDocumentFieldContribution>> contributions = new HashMap<>();

	public void add(LuceneNestedDocumentFieldContribution contribution) {
		if ( contribution == null ) {
			return;
		}

		contributions
				.computeIfAbsent( contribution.getNestedDocumentPath(), ignored -> new HashSet<>() )
				.add( contribution );
	}

	public boolean isEmpty() {
		return contributions.isEmpty();
	}

	public void processNestedPaths(Query luceneQuery, IndexSearcher indexSearcher, ScoreDoc[] scoreDocs) throws IOException {
		for ( Map.Entry<String, Set<LuceneNestedDocumentFieldContribution>> fieldEntry : contributions.entrySet() ) {
			String nestedDocumentPath = fieldEntry.getKey();
			Map<Integer, Set<Integer>> nestedDocumentMap = getFetchNestedDocumentMap( nestedDocumentPath, luceneQuery, indexSearcher, scoreDocs );
			for ( LuceneNestedDocumentFieldContribution fieldContribution : fieldEntry.getValue() ) {
				fieldContribution.setNestedDocumentMap( nestedDocumentMap );
			}
		}
	}

	private Map<Integer, Set<Integer>> getFetchNestedDocumentMap(String nestedDocumentPath, Query luceneQuery, IndexSearcher indexSearcher,
			ScoreDoc[] scoreDocs) throws IOException {

		BooleanQuery childQuery = LuceneNestedQueries.findChildQuery( Collections.singleton( nestedDocumentPath ), luceneQuery );
		LuceneChildrenCollector childrenCollector = new LuceneChildrenCollector();
		indexSearcher.search( childQuery, childrenCollector );

		Map<String, Set<Integer>> children = childrenCollector.getChildren();
		Map<Integer, Set<Integer>> result = new HashMap<>();

		// TODO HSEARCH-3657 this could be avoided
		for ( ScoreDoc hit : scoreDocs ) {
			Document doc = indexSearcher.doc( hit.doc, ID_FIELD_SET );
			String parentId = doc.getField( LuceneFields.idFieldName() ).stringValue();
			if ( parentId == null ) {
				continue;
			}
			Set<Integer> childIds = children.get( parentId );
			if ( childIds == null ) {
				continue;
			}
			result.put( hit.doc, childIds );
		}

		return result;
	}
}
