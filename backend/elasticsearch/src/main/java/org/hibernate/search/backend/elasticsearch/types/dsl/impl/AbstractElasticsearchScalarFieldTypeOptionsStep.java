/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchStandardFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;

abstract class AbstractElasticsearchScalarFieldTypeOptionsStep<S extends AbstractElasticsearchScalarFieldTypeOptionsStep<?, F>, F>
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchScalarFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, String dataType) {
		super( buildContext, fieldType, dataType );
	}

	@Override
	protected final ElasticsearchIndexFieldType<F> toIndexFieldType(PropertyMapping mapping) {
		ElasticsearchFieldCodec<F> codec = complete( mapping );

		return new ElasticsearchIndexFieldType<>(
				getFieldType(), codec,
				createDslConverter(), createRawDslConverter(),
				createProjectionConverter(), createRawProjectionConverter(),
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( resolvedSearchable, codec ),
				createFieldSortBuilderFactory( resolvedSortable, codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec ),
				createAggregationBuilderFactory( resolvedAggregable, codec ),
				mapping
		);
	}

	protected ElasticsearchStandardFieldSortBuilderFactory<F> createFieldSortBuilderFactory(
			boolean resolvedAggregable, ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedAggregable, codec );
	}

	protected ElasticsearchFieldAggregationBuilderFactory<F> createAggregationBuilderFactory(
			boolean resolvedAggregable, ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchStandardFieldAggregationBuilderFactory<>(
				resolvedAggregable,
				codec
		);
	}

	protected abstract ElasticsearchFieldCodec<F> complete(PropertyMapping mapping);
}
