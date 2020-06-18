/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;

public class ElasticsearchCompositeSort extends AbstractElasticsearchSort {

	private final List<ElasticsearchSearchSort> elements;

	protected ElasticsearchCompositeSort(Builder builder) {
		super( builder );
		elements = builder.elements;
		// Ensure illegal attempts to mutate the sort will fail
		builder.elements = null;
	}

	@Override
	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		for ( ElasticsearchSearchSort element : elements ) {
			element.toJsonSorts( collector );
		}
	}

	public static class Builder extends AbstractBuilder implements CompositeSortBuilder {
		private List<ElasticsearchSearchSort> elements = new ArrayList<>();

		protected Builder(ElasticsearchSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public void add(SearchSort sort) {
			elements.add( ElasticsearchSearchSort.from( searchContext, sort ) );
		}

		@Override
		public SearchSort build() {
			return new ElasticsearchCompositeSort( this );
		}
	}
}
