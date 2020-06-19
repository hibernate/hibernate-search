/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

public interface LuceneSearchSortBuilderFactory
		extends SearchSortBuilderFactory<LuceneSearchSortCollector> {

	LuceneSearchSort fromLuceneSortField(SortField luceneSortField);

	LuceneSearchSort fromLuceneSort(Sort luceneSort);

}
