/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.PrefixPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;

public class LuceneTextPrefixPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneTextPrefixPredicate(Builder<?> builder) {
		super( builder );
	}

	public static class Factory<F> extends AbstractLuceneValueFieldSearchQueryElementFactory<PrefixPredicateBuilder, F> {
		@Override
		public PrefixPredicateBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements PrefixPredicateBuilder {

		private final Analyzer analyzerOrNormalizer;

		private String prefix;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.analyzerOrNormalizer = field.type().searchAnalyzerOrNormalizer();
		}

		@Override
		public void prefix(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextPrefixPredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			return new PrefixQuery(
					new Term( absoluteFieldPath, analyzerOrNormalizer.normalize( absoluteFieldPath, prefix ) ) );
		}
	}
}
