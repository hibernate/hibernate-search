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

}
