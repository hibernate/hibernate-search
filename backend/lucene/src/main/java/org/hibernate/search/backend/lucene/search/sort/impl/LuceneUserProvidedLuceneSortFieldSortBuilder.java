/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.types.sort.impl.AbstractUserProvidedLuceneFieldSortBuilder;

class LuceneUserProvidedLuceneSortFieldSortBuilder extends AbstractUserProvidedLuceneFieldSortBuilder implements LuceneSearchSortBuilder {

	private final SortField luceneSortField;
	private final String nestedDocumentPath;

	LuceneUserProvidedLuceneSortFieldSortBuilder(SortField luceneSortField, String nestedDocumentPath) {
		super();
		this.luceneSortField = luceneSortField;
		this.nestedDocumentPath = nestedDocumentPath;
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		collectSortField( collector, luceneSortField, nestedDocumentPath );
	}
}
