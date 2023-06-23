/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public final class LucenePredicateTypeKeys {

	private LucenePredicateTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<LuceneSimpleQueryStringPredicateBuilderFieldState> SIMPLE_QUERY_STRING =
			PredicateTypeKeys.key( "simple-query-string" );

}
