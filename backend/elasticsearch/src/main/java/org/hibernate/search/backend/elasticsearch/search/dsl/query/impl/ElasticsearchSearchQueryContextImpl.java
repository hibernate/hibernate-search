/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query.impl;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortContainerContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractDelegatingSearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryContextImplementor;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public class ElasticsearchSearchQueryContextImpl<T>
		extends AbstractDelegatingSearchQueryContext<
				ElasticsearchSearchQueryContext<T>,
				T,
				ElasticsearchSearchPredicateFactoryContext,
				ElasticsearchSearchSortContainerContext
				>
		implements ElasticsearchSearchQueryResultContext<T>, ElasticsearchSearchQueryContext<T> {

	// FIXME use the builder to make toQuery return an Elasticsearch-specific query type
	private final ElasticsearchSearchQueryBuilder<T> builder;

	public ElasticsearchSearchQueryContextImpl(SearchQueryContextImplementor<?, T, ?, ?> original,
			ElasticsearchSearchQueryBuilder<T> builder) {
		super( original );
		this.builder = builder;
	}

	@Override
	protected ElasticsearchSearchQueryContextImpl<T> thisAsS() {
		return this;
	}

	@Override
	protected ElasticsearchSearchPredicateFactoryContext extendPredicateContext(
			SearchPredicateFactoryContext predicateFactoryContext) {
		return predicateFactoryContext.extension( ElasticsearchExtension.get() );
	}

	@Override
	protected ElasticsearchSearchSortContainerContext extendSortContext(
			SearchSortContainerContext sortContainerContext) {
		return sortContainerContext.extension( ElasticsearchExtension.get() );
	}
}
