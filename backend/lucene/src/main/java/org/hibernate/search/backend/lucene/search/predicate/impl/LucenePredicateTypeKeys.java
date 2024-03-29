/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class LucenePredicateTypeKeys {

	private LucenePredicateTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<LuceneCommonQueryStringPredicateBuilderFieldState> SIMPLE_QUERY_STRING =
			of( IndexFieldTraits.Predicates.SIMPLE_QUERY_STRING );
	public static final SearchQueryElementTypeKey<LuceneCommonQueryStringPredicateBuilderFieldState> QUERY_STRING =
			of( IndexFieldTraits.Predicates.QUERY_STRING );

}
