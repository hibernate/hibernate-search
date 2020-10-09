/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;

public final class LuceneSimpleQueryStringPredicateBuilderFieldState
	implements SimpleQueryStringPredicateBuilder.FieldState {

	private final LuceneSearchValueFieldContext<?> field;
	private Float boost;

	private LuceneSimpleQueryStringPredicateBuilderFieldState(LuceneSearchValueFieldContext<?> field) {
		this.field = field;
	}

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	public Analyzer getAnalyzerOrNormalizer() {
		return field.type().searchAnalyzerOrNormalizer();
	}

	public Float getBoost() {
		return boost;
	}

	public static class Factory
			extends
			AbstractLuceneSearchValueFieldQueryElementFactory<LuceneSimpleQueryStringPredicateBuilderFieldState, String> {
		@Override
		public LuceneSimpleQueryStringPredicateBuilderFieldState create(LuceneSearchContext searchContext,
				LuceneSearchValueFieldContext<String> field) {
			return new LuceneSimpleQueryStringPredicateBuilderFieldState( field );
		}
	}

}
