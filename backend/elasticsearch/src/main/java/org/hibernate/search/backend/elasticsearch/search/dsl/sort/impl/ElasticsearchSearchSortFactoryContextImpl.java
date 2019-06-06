/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.DelegatingSearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.StaticNonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


public class ElasticsearchSearchSortFactoryContextImpl
		extends DelegatingSearchSortFactoryContext
		implements ElasticsearchSearchSortFactoryContext {

	private final SearchSortDslContext<ElasticsearchSearchSortBuilderFactory, ElasticsearchSearchSortBuilder> dslContext;

	public ElasticsearchSearchSortFactoryContextImpl(SearchSortFactoryContext delegate,
			SearchSortDslContext<ElasticsearchSearchSortBuilderFactory, ElasticsearchSearchSortBuilder> dslContext) {
		super( delegate );
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext fromJson(String jsonString) {
		return staticNonEmptyContext( dslContext.getFactory().fromJson( jsonString ) );
	}

	private NonEmptySortContext staticNonEmptyContext(ElasticsearchSearchSortBuilder builder) {
		return new StaticNonEmptySortContext<>( dslContext, builder );
	}
}
