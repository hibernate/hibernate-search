/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import org.apache.lucene.search.SortField;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;


class UserProvidedLuceneSortFieldSortContributor implements SearchSortContributor<LuceneSearchSortCollector> {

	private final SortField sortField;

	UserProvidedLuceneSortFieldSortContributor(SortField sortField) {
		this.sortField = sortField;
	}

	@Override
	public void contribute(LuceneSearchSortCollector collector) {
		collector.collectSortField( sortField );
	}
}
