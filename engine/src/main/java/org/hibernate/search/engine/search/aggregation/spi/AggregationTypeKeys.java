/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class AggregationTypeKeys {

	private AggregationTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<TermsAggregationBuilder.TypeSelector> TERMS =
			of( IndexFieldTraits.Aggregations.TERMS );
	public static final SearchQueryElementTypeKey<RangeAggregationBuilder.TypeSelector> RANGE =
			of( IndexFieldTraits.Aggregations.RANGE );
	public static final SearchQueryElementTypeKey<FieldMetricAggregationBuilder.TypeSelector> SUM =
			of( IndexFieldTraits.Aggregations.SUM );
	public static final SearchQueryElementTypeKey<FieldMetricAggregationBuilder.TypeSelector> MIN =
			of( IndexFieldTraits.Aggregations.MIN );
	public static final SearchQueryElementTypeKey<FieldMetricAggregationBuilder.TypeSelector> MAX =
			of( IndexFieldTraits.Aggregations.MAX );
	public static final SearchQueryElementTypeKey<SearchFilterableAggregationBuilder<Long>> COUNT_VALUES =
			of( IndexFieldTraits.Aggregations.COUNT_VALUES );
	public static final SearchQueryElementTypeKey<SearchFilterableAggregationBuilder<Long>> COUNT_DISTINCT_VALUES =
			of( IndexFieldTraits.Aggregations.COUNT_DISTINCT_VALUES );
	public static final SearchQueryElementTypeKey<FieldMetricAggregationBuilder.TypeSelector> AVG =
			of( IndexFieldTraits.Aggregations.AVG );
	public static final SearchQueryElementTypeKey<CountDocumentAggregationBuilder.TypeSelector> COUNT_DOCUMENTS =
			of( IndexFieldTraits.Aggregations.COUNT_DOCUMENTS );


}
