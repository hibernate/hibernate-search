/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.search.sort.SearchSort;

class ElasticsearchSearchSort implements SearchSort, ElasticsearchSearchSortBuilder {

	private final List<ElasticsearchSearchSortBuilder> delegates;
	private final Set<String> indexNames;

	ElasticsearchSearchSort(List<ElasticsearchSearchSortBuilder> delegates, Set<String> indexName) {
		this.delegates = delegates;
		this.indexNames = indexName;
	}

	@Override
	public void buildAndAddTo(ElasticsearchSearchSortCollector collector) {
		for ( ElasticsearchSearchSortBuilder delegate : delegates ) {
			delegate.buildAndAddTo( collector );
		}
	}

	public Set<String> getIndexNames() {
		return indexNames;
	}
}
