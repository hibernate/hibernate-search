/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.ElasticsearchSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.DelegatingSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;

public class ElasticsearchSearchAggregationFactoryImpl
		extends DelegatingSearchAggregationFactory
		implements ElasticsearchSearchAggregationFactory {

	private final SearchAggregationDslContext<ElasticsearchSearchAggregationBuilderFactory> dslContext;

	public ElasticsearchSearchAggregationFactoryImpl(SearchAggregationFactory delegate,
			SearchAggregationDslContext<ElasticsearchSearchAggregationBuilderFactory> dslContext) {
		super( delegate );
		this.dslContext = dslContext;
	}

	// TODO HSEARCH-3661/HSEARCH-3662 implement extensions to the aggregation DSL for Elasticsearch

}
