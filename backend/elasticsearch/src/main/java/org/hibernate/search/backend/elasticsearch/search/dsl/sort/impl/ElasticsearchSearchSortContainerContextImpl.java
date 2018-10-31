/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortContainerContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.DelegatingSearchSortContainerContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.NonEmptySortContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


public class ElasticsearchSearchSortContainerContextImpl
		extends DelegatingSearchSortContainerContextImpl
		implements ElasticsearchSearchSortContainerContext {

	private final ElasticsearchSearchSortBuilderFactory factory;

	private final SearchSortDslContext<? super ElasticsearchSearchSortBuilder> dslContext;

	public ElasticsearchSearchSortContainerContextImpl(SearchSortContainerContext delegate,
			ElasticsearchSearchSortBuilderFactory factory,
			SearchSortDslContext<? super ElasticsearchSearchSortBuilder> dslContext) {
		super( delegate );
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext fromJsonString(String jsonString) {
		dslContext.addChild( factory.fromJsonString( jsonString ) );
		return nonEmptyContext();
	}

	private NonEmptySortContext nonEmptyContext() {
		return new NonEmptySortContextImpl( this, dslContext );
	}
}
