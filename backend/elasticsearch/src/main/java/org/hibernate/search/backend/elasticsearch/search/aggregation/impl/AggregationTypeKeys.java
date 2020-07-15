/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.SearchQueryElementTypeKey;

public final class AggregationTypeKeys {

	private AggregationTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<ElasticsearchTermsAggregation.TypeSelector<?>> TERMS = key( "terms" );
	public static final SearchQueryElementTypeKey<ElasticsearchRangeAggregation.TypeSelector<?>> RANGE = key( "range" );

	private static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "aggregation", name );
	}

}
