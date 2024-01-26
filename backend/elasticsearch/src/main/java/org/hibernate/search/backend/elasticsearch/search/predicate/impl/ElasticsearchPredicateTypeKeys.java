/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class ElasticsearchPredicateTypeKeys {

	private ElasticsearchPredicateTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<
			ElasticsearchCommonQueryStringPredicateBuilderFieldState> SIMPLE_QUERY_STRING =
					SearchQueryElementTypeKey.of( IndexFieldTraits.Predicates.SIMPLE_QUERY_STRING );
	public static final SearchQueryElementTypeKey<
			ElasticsearchCommonQueryStringPredicateBuilderFieldState> QUERY_STRING =
					SearchQueryElementTypeKey.of( IndexFieldTraits.Predicates.QUERY_STRING );
}
