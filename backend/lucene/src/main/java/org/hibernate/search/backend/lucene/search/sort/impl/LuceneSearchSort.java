/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.util.List;

import org.apache.lucene.search.SortField;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;

class LuceneSearchSort implements SearchSort, SearchSortContributor<LuceneSearchSortCollector> {

	private final List<SortField> sortFields;

	LuceneSearchSort(List<SortField> sortFields) {
		this.sortFields = sortFields;
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector) {
		collector.collectSortFields( sortFields );
	}

}
