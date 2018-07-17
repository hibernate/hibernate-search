/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortContainerContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortFactory;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.DelegatingSearchSortContainerContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


public class ElasticsearchSearchSortContainerContextImpl<N>
		extends DelegatingSearchSortContainerContextImpl<N>
		implements ElasticsearchSearchSortContainerContext<N> {

	private final ElasticsearchSearchSortFactory factory;

	private final SearchSortDslContext<N, ? super ElasticsearchSearchSortBuilder> dslContext;

	public ElasticsearchSearchSortContainerContextImpl(SearchSortContainerContext<N> delegate,
			ElasticsearchSearchSortFactory factory,
			SearchSortDslContext<N, ? super ElasticsearchSearchSortBuilder> dslContext) {
		super( delegate );
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext<N> fromJsonString(String jsonString) {
		dslContext.addChild( factory.fromJsonString( jsonString ) );
		return nonEmptyContext();
	}

	private NonEmptySortContext<N> nonEmptyContext() {
		return new NonEmptySortContext<N>() {
			@Override
			public SearchSortContainerContext<N> then() {
				return ElasticsearchSearchSortContainerContextImpl.this;
			}

			@Override
			public N end() {
				return dslContext.getNextContext();
			}
		};
	}
}
