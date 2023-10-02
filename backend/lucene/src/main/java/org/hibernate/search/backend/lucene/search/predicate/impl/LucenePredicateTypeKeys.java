/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
