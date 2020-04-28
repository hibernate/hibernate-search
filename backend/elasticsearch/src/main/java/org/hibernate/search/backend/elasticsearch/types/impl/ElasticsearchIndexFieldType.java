/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.IndexFieldType;

public class ElasticsearchIndexFieldType<F> implements IndexFieldType<F> {
	private final Class<F> valueType;
	private final ElasticsearchFieldCodec<F> codec;
	private final ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory;
	private final ElasticsearchFieldSortBuilderFactory sortBuilderFactory;
	private final ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory;
	private final ElasticsearchFieldAggregationBuilderFactory aggregationBuilderFactory;
	private final PropertyMapping mapping;

	public ElasticsearchIndexFieldType(Class<F> valueType, ElasticsearchFieldCodec<F> codec,
			ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory,
			ElasticsearchFieldAggregationBuilderFactory aggregationBuilderFactory,
			PropertyMapping mapping) {
		this.valueType = valueType;
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.aggregationBuilderFactory = aggregationBuilderFactory;
		this.mapping = mapping;
	}

	@Override
	public String toString() {
		return mapping.toString();
	}

	public Class<F> getValueType() {
		return valueType;
	}

	public ElasticsearchFieldCodec<F> getCodec() {
		return codec;
	}

	public ElasticsearchFieldPredicateBuilderFactory getPredicateBuilderFactory() {
		return predicateBuilderFactory;
	}

	public ElasticsearchFieldSortBuilderFactory getSortBuilderFactory() {
		return sortBuilderFactory;
	}

	public ElasticsearchFieldProjectionBuilderFactory getProjectionBuilderFactory() {
		return projectionBuilderFactory;
	}

	public ElasticsearchFieldAggregationBuilderFactory getAggregationBuilderFactory() {
		return aggregationBuilderFactory;
	}

	public PropertyMapping getMapping() {
		return mapping;
	}
}
