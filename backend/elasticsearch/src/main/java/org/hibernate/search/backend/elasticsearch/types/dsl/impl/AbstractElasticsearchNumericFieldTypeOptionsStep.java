/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchMetricFieldAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchMetricLongAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchRangeAggregation;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchPredicateTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchRangePredicate;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardMatchPredicate;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchTermsPredicate;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSort;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

abstract class AbstractElasticsearchNumericFieldTypeOptionsStep<
		S extends AbstractElasticsearchNumericFieldTypeOptionsStep<?, F>,
		F>
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchNumericFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType, String dataType, DefaultStringConverters.Converter<F> defaultConverter) {
		super( buildContext, fieldType, dataType, defaultConverter );
	}

	@Override
	protected final void complete() {
		ElasticsearchFieldCodec<F> codec = completeCodec( buildContext );
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.MATCH,
					new ElasticsearchStandardMatchPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.RANGE, new ElasticsearchRangePredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.Factory<>() );
			builder.queryElementFactory( PredicateTypeKeys.TERMS, new ElasticsearchTermsPredicate.Factory<>( codec ) );
			builder.queryElementFactory( ElasticsearchPredicateTypeKeys.SIMPLE_QUERY_STRING,
					new ElasticsearchCommonQueryStringPredicateBuilderFieldState.Factory<>( codec ) );
			builder.queryElementFactory( ElasticsearchPredicateTypeKeys.QUERY_STRING,
					new ElasticsearchCommonQueryStringPredicateBuilderFieldState.Factory<>( codec ) );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.FIELD, new ElasticsearchStandardFieldSort.Factory<>( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new ElasticsearchTermsAggregation.Factory<>( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.RANGE, new ElasticsearchRangeAggregation.Factory<>( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.SUM, ElasticsearchMetricFieldAggregation.sum( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.MIN, ElasticsearchMetricFieldAggregation.min( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.MAX, ElasticsearchMetricFieldAggregation.max( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.AVG, ElasticsearchMetricFieldAggregation.avg( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.COUNT_VALUES, ElasticsearchMetricLongAggregation.count( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.COUNT_DISTINCT_VALUES,
					ElasticsearchMetricLongAggregation.countDistinct( codec ) );
		}
	}

	protected abstract ElasticsearchFieldCodec<F> completeCodec(ElasticsearchIndexFieldTypeBuildContext buildContext);
}
