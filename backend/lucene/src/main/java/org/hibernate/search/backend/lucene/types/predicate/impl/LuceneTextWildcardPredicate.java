/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.predicate.parse.impl.LuceneWildcardExpressionHelper;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;

class LuceneTextWildcardPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneTextWildcardPredicate(Builder builder) {
		super( builder );
	}

	static class Builder<F> extends AbstractBuilder<F> implements WildcardPredicateBuilder {

		private final Analyzer analyzerOrNormalizer;

		private String pattern;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
			super( searchContext, field );
			this.analyzerOrNormalizer = field.type().searchAnalyzerOrNormalizer();
		}

		@Override
		public void pattern(String wildcardPattern) {
			this.pattern = wildcardPattern;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextWildcardPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			BytesRef analyzedWildcard = LuceneWildcardExpressionHelper.analyzeWildcard( analyzerOrNormalizer, absoluteFieldPath, pattern );
			return new WildcardQuery( new Term( absoluteFieldPath, analyzedWildcard ) );
		}
	}
}
