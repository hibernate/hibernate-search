/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.search.dsl.sort.SortThenStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.search.dsl.sort.spi.DelegatingSearchSortFactory;
import org.hibernate.search.engine.search.dsl.sort.spi.StaticSortThenStep;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


public class ElasticsearchSearchSortFactoryImpl
		extends DelegatingSearchSortFactory
		implements ElasticsearchSearchSortFactory {

	private final SearchSortDslContext<ElasticsearchSearchSortBuilderFactory, ElasticsearchSearchSortBuilder> dslContext;

	public ElasticsearchSearchSortFactoryImpl(SearchSortFactory delegate,
			SearchSortDslContext<ElasticsearchSearchSortBuilderFactory, ElasticsearchSearchSortBuilder> dslContext) {
		super( delegate );
		this.dslContext = dslContext;
	}

	@Override
	public SortThenStep fromJson(String jsonString) {
		return staticThenStep( dslContext.getBuilderFactory().fromJson( jsonString ) );
	}

	private SortThenStep staticThenStep(ElasticsearchSearchSortBuilder builder) {
		return new StaticSortThenStep<>( dslContext, builder );
	}
}
