/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.search.sort.SearchSort;

class LuceneSearchSort implements SearchSort, LuceneSearchSortBuilder {

	private final Set<String> indexNames;
	private final List<LuceneSearchSortBuilder> builders;

	LuceneSearchSort(Set<String> indexNames, List<LuceneSearchSortBuilder> builders) {
		this.indexNames = indexNames;
		this.builders = builders;
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		for ( LuceneSearchSortBuilder builder : builders ) {
			builder.buildAndContribute( collector );
		}
	}

	public Set<String> getIndexNames() {
		return indexNames;
	}
}
