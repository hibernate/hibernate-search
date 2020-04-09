/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilderFactory;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.DelegatingSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.StaticSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;

import com.google.gson.JsonObject;


public class ElasticsearchSearchSortFactoryImpl
		extends DelegatingSearchSortFactory<ElasticsearchSearchPredicateFactory>
		implements ElasticsearchSearchSortFactory {

	private final SearchSortDslContext<ElasticsearchSearchSortBuilderFactory, ElasticsearchSearchSortBuilder, ?> dslContext;

	public ElasticsearchSearchSortFactoryImpl(SearchSortFactory delegate,
			SearchSortDslContext<ElasticsearchSearchSortBuilderFactory, ElasticsearchSearchSortBuilder, ElasticsearchSearchPredicateFactory> dslContext) {
		super( delegate, dslContext );
		this.dslContext = dslContext;
	}

	@Override
	public SortThenStep fromJson(JsonObject jsonObject) {
		return staticThenStep( dslContext.getBuilderFactory().fromJson( jsonObject ) );
	}

	@Override
	public SortThenStep fromJson(String jsonString) {
		return staticThenStep( dslContext.getBuilderFactory().fromJson( jsonString ) );
	}

	private SortThenStep staticThenStep(ElasticsearchSearchSortBuilder builder) {
		return new StaticSortThenStep<>( dslContext, builder );
	}
}
