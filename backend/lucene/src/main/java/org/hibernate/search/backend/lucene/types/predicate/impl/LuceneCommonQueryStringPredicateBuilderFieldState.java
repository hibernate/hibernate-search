/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public static class Factory<T>
			extends
			AbstractLuceneValueFieldSearchQueryElementFactory<LuceneCommonQueryStringPredicateBuilderFieldState, T> {
		@Override
		public LuceneCommonQueryStringPredicateBuilderFieldState create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<T> field) {
			return new LuceneCommonQueryStringPredicateBuilderFieldState( field );
		}
	}

}
