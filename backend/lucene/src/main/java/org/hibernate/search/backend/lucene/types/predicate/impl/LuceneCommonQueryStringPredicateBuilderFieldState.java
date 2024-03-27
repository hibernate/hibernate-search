/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;

public final class LuceneCommonQueryStringPredicateBuilderFieldState
		implements CommonQueryStringPredicateBuilder.FieldState {

	private final LuceneSearchIndexValueFieldContext<?> field;
	private Float boost;

	private LuceneCommonQueryStringPredicateBuilderFieldState(LuceneSearchIndexValueFieldContext<?> field) {
		this.field = field;
	}

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	public LuceneSearchIndexValueFieldContext<?> field() {
		return field;
	}

	public Float boost() {
		return boost;
	}

	public static class Factory
			extends
			AbstractLuceneValueFieldSearchQueryElementFactory<LuceneCommonQueryStringPredicateBuilderFieldState, String> {
		@Override
		public LuceneCommonQueryStringPredicateBuilderFieldState create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<String> field) {
			return new LuceneCommonQueryStringPredicateBuilderFieldState( field );
		}
	}

}
