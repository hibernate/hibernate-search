/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;

import org.apache.lucene.search.SortField;

class LuceneUserProvidedLuceneSortFieldSort extends AbstractLuceneSort {

	private final SortField luceneSortField;

	LuceneUserProvidedLuceneSortFieldSort(LuceneSearchIndexScope<?> scope, SortField luceneSortField) {
		super( scope );
		this.luceneSortField = luceneSortField;
	}

	@Override
	public void toSortFields(LuceneSearchSortCollector collector) {
		collector.collectSortField( luceneSortField );
	}
}
