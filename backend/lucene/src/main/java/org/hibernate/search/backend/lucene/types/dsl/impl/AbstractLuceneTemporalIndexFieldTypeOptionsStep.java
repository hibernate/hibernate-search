/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.lucene.search.predicate.impl.LucenePredicateTypeKeys;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneAvgNumericFieldAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneCountDistinctNumericLongAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneCountNumericLongAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneMaxNumericFieldAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneMinNumericFieldAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericRangeAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericTermsAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneSumNumericFieldAggregation;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneExistsPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericMatchPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericRangePredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericTermsPredicate;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneStandardFieldSort;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultStringConverters;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;

abstract class AbstractLuceneTemporalIndexFieldTypeOptionsStep<
		S extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<S, F>,
		F extends TemporalAccessor>
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<S, F> {

	private Sortable sortable = Sortable.DEFAULT;

	AbstractLuceneTemporalIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext, Class<F> fieldType,
			DefaultStringConverters.Converter<F> defaultConverter) {
		super( buildContext, fieldType, defaultConverter );
	}

	@Override
	public S sortable(Sortable sortable) {
		this.sortable = sortable;
		return thisAsS();
	}

	@Override
	public LuceneIndexValueFieldType<F> toIndexFieldType() {
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedAggregable = resolveDefault( aggregable );

		Indexing indexing = resolvedSearchable ? Indexing.ENABLED : Indexing.DISABLED;
		DocValues docValues = resolvedSortable || resolvedAggregable ? DocValues.ENABLED : DocValues.DISABLED;
		Storage storage = resolvedProjectable ? Storage.ENABLED : Storage.DISABLED;

		AbstractLuceneNumericFieldCodec<F, ?> codec = createCodec( indexing, docValues, storage, indexNullAsValue );
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.MATCH, new LuceneNumericMatchPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.RANGE, new LuceneNumericRangePredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.TERMS, new LuceneNumericTermsPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS,
					DocValues.ENABLED.equals( docValues )
							? new LuceneExistsPredicate.DocValuesOrNormsBasedFactory<>()
							: new LuceneExistsPredicate.DefaultFactory<>() );
			builder.queryElementFactory( LucenePredicateTypeKeys.SIMPLE_QUERY_STRING,
					new LuceneCommonQueryStringPredicateBuilderFieldState.Factory<>( codec ) );
			builder.queryElementFactory( LucenePredicateTypeKeys.QUERY_STRING,
					new LuceneCommonQueryStringPredicateBuilderFieldState.Factory<>( codec ) );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.FIELD,
					new LuceneStandardFieldSort.TemporalFieldFactory<>( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new LuceneFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new LuceneNumericTermsAggregation.Factory<>( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.RANGE, new LuceneNumericRangeAggregation.Factory<>( codec ) );

			if ( sumAggregationSupported() ) {
				builder.queryElementFactory( AggregationTypeKeys.SUM, LuceneSumNumericFieldAggregation.factory( codec ) );
			}
			builder.queryElementFactory( AggregationTypeKeys.MIN, LuceneMinNumericFieldAggregation.factory( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.MAX, LuceneMaxNumericFieldAggregation.factory( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.COUNT_VALUES, LuceneCountNumericLongAggregation.factory( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.COUNT_DISTINCT_VALUES,
					LuceneCountDistinctNumericLongAggregation.factory( codec ) );
			builder.queryElementFactory( AggregationTypeKeys.AVG, LuceneAvgNumericFieldAggregation.factory( codec ) );
		}

		return builder.build();
	}

	protected abstract AbstractLuceneNumericFieldCodec<F, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, F indexNullAsValue);

	protected boolean sumAggregationSupported() {
		return true;
	}
}
