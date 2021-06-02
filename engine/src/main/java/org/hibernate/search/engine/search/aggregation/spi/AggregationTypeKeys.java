/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class AggregationTypeKeys {

	private AggregationTypeKeys() {
	}

	public static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "aggregation", name );
	}

	public static final SearchQueryElementTypeKey<TermsAggregationBuilder.TypeSelector> TERMS = key( "terms" );
	public static final SearchQueryElementTypeKey<RangeAggregationBuilder.TypeSelector> RANGE = key( "range" );

}
