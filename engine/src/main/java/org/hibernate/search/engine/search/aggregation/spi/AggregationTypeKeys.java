/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
