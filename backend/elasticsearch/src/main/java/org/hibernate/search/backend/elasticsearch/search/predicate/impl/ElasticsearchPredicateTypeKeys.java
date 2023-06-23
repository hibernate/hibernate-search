/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class ElasticsearchPredicateTypeKeys {

	private ElasticsearchPredicateTypeKeys() {
	}

	private static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "predicate", name );
	}

	public static final SearchQueryElementTypeKey<
			ElasticsearchSimpleQueryStringPredicateBuilderFieldState> SIMPLE_QUERY_STRING =
					key( "simple-query-string" );

}
