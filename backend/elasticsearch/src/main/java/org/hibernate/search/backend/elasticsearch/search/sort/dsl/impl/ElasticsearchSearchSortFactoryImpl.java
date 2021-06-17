/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.dsl.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSort;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.dsl.spi.StaticSortThenStep;

import com.google.gson.JsonObject;


public class ElasticsearchSearchSortFactoryImpl
		extends AbstractSearchSortFactory<
						ElasticsearchSearchSortFactory,
						ElasticsearchSearchSortIndexScope<?>,
						ElasticsearchSearchPredicateFactory
				>
		implements ElasticsearchSearchSortFactory {

	public ElasticsearchSearchSortFactoryImpl(SearchSortDslContext<ElasticsearchSearchSortIndexScope<?>, ElasticsearchSearchPredicateFactory> dslContext) {
		super( dslContext );
	}

	@Override
	public SortThenStep fromJson(JsonObject jsonObject) {
		return staticThenStep( dslContext.scope().sortBuilders().fromJson( jsonObject ) );
	}

	@Override
	public SortThenStep fromJson(String jsonString) {
		return staticThenStep( dslContext.scope().sortBuilders().fromJson( jsonString ) );
	}

	private SortThenStep staticThenStep(ElasticsearchSearchSort sort) {
		return new StaticSortThenStep( dslContext, sort );
	}
}
