/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;

import org.apache.lucene.search.Sort;

class LuceneUserProvidedLuceneSortSort extends AbstractLuceneSort {

	private final Sort luceneSort;

	LuceneUserProvidedLuceneSortSort(LuceneSearchIndexScope<?> scope, Sort luceneSort) {
		super( scope );
		this.luceneSort = luceneSort;
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		collector.collectSortFields( luceneSort.getSort() );
	}
}
