/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import org.hibernate.search.backend.lucene.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.lucene.types.aggregation.impl.AbstractLuceneFacetsBasedTermsAggregation;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneNumericRangeAggregation;

public final class AggregationTypeKeys {

	private AggregationTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<AbstractLuceneFacetsBasedTermsAggregation.AbstractTypeSelector<?>> TERMS = key( "terms" );
	public static final SearchQueryElementTypeKey<LuceneNumericRangeAggregation.TypeSelector<?>> RANGE = key( "range" );

	private static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "aggregation", name );
	}

}
