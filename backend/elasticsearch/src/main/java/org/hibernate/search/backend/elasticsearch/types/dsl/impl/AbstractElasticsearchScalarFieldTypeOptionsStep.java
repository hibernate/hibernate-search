/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchStandardFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;

abstract class AbstractElasticsearchScalarFieldTypeOptionsStep<S extends AbstractElasticsearchScalarFieldTypeOptionsStep<?, F>, F>
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchScalarFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, String dataType) {
		super( buildContext, fieldType, dataType );
	}

	@Override
	protected final ElasticsearchIndexFieldType<F> toIndexFieldType(PropertyMapping mapping) {
		ElasticsearchFieldCodec<F> codec = complete( mapping );

		DslConverter<?, ? extends F> dslConverter = createDslConverter();
		DslConverter<F, ? extends F> rawDslConverter = createRawDslConverter();
		ProjectionConverter<? super F, ?> projectionConverter = createProjectionConverter();
		ProjectionConverter<? super F, F> rawProjectionConverter = createRawProjectionConverter();

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>(
						resolvedSearchable, dslConverter, rawDslConverter, codec
				),
				new ElasticsearchStandardFieldSortBuilderFactory<>(
						resolvedSortable, dslConverter, rawDslConverter, codec
				),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>(
						resolvedProjectable, projectionConverter, rawProjectionConverter, codec
				),
				createAggregationBuilderFactory(
						resolvedAggregable,
						dslConverter, rawDslConverter,
						projectionConverter, rawProjectionConverter,
						codec
				),
				mapping
		);
	}

	protected ElasticsearchFieldAggregationBuilderFactory createAggregationBuilderFactory(
			boolean resolvedAggregable,
			DslConverter<?,? extends F> dslToIndexConverter,
			DslConverter<F,? extends F> rawDslToIndexConverter,
			ProjectionConverter<? super F,?> indexToProjectionConverter,
			ProjectionConverter<? super F,F> rawIndexToProjectionConverter,
			ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchStandardFieldAggregationBuilderFactory<>(
				resolvedAggregable,
				dslToIndexConverter,
				rawDslToIndexConverter,
				indexToProjectionConverter,
				rawIndexToProjectionConverter,
				codec
		);
	}

	protected abstract ElasticsearchFieldCodec<F> complete(PropertyMapping mapping);
}
