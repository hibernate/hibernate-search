/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
				ElasticsearchSearchPredicateFactory>
		implements ElasticsearchSearchSortFactory {

	public ElasticsearchSearchSortFactoryImpl(
			SearchSortDslContext<ElasticsearchSearchSortIndexScope<?>, ElasticsearchSearchPredicateFactory> dslContext) {
		super( dslContext );
	}

	@Override
	public ElasticsearchSearchSortFactory withRoot(String objectFieldPath) {
		return new ElasticsearchSearchSortFactoryImpl( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ),
				dslContext.predicateFactory().withRoot( objectFieldPath ) ) );
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
