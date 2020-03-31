/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.util.Map;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.types.sort.impl.AbstractUserProvidedLuceneFieldSortBuilder;


class LuceneUserProvidedLuceneSortSortBuilder extends AbstractUserProvidedLuceneFieldSortBuilder implements LuceneSearchSortBuilder {

	private final Sort luceneSort;
	private final Map<String, String> nestedDocumentPaths;

	LuceneUserProvidedLuceneSortSortBuilder(Sort luceneSort, Map<String, String> nestedDocumentPaths) {
		this.luceneSort = luceneSort;
		this.nestedDocumentPaths = nestedDocumentPaths;
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		SortField[] sort = luceneSort.getSort();
		for ( SortField sortField : sort ) {
			String nestedDocumentPath = nestedDocumentPaths.get( sortField.getField() );
			collectSortField( collector, sortField, nestedDocumentPath );
		}
	}
}
