/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;

public abstract class AbstractElasticsearchSort implements ElasticsearchSearchSort {

	protected final Set<String> indexNames;

	protected AbstractElasticsearchSort(AbstractBuilder builder) {
		this( builder.searchContext );
	}

	protected AbstractElasticsearchSort(ElasticsearchSearchContext searchContext) {
		indexNames = searchContext.indexes().hibernateSearchIndexNames();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder implements SearchSortBuilder {

		protected final ElasticsearchSearchContext searchContext;

		protected AbstractBuilder(ElasticsearchSearchContext searchContext) {
			this.searchContext = searchContext;
		}
	}
}
