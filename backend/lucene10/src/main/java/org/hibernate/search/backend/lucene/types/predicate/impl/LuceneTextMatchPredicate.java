/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.AnalysisLog;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.FuzzyQueryBuilder;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneCommonMinimumShouldMatchConstraints;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

public class LuceneTextMatchPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneTextMatchPredicate(Builder<?> builder) {
		super( builder );
	}

	public static class Factory<F>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<MatchPredicateBuilder, F, LuceneFieldCodec<F, String>> {
		public Factory(LuceneFieldCodec<F, String> codec) {
			super( codec );
		}

		@Override
		public Builder<F> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements MatchPredicateBuilder {
		private final LuceneFieldCodec<F, String> codec;
		private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
		private final LuceneCommonMinimumShouldMatchConstraints minimumShouldMatchConstraints;

		private String value;

		private Integer maxEditDistance;
		private Integer prefixLength;

		private Analyzer overrideAnalyzerOrNormalizer;

		private Builder(LuceneFieldCodec<F, String> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
			this.analysisDefinitionRegistry = scope.analysisDefinitionRegistry();
			this.minimumShouldMatchConstraints = new LuceneCommonMinimumShouldMatchConstraints();
		}

		@Override
		public void value(Object value, ValueModel valueModel) {
			this.value = convertAndEncode( codec, value, valueModel );
		}

		@Override
		public void fuzzy(int maxEditDistance, int exactPrefixLength) {
			this.maxEditDistance = maxEditDistance;
			this.prefixLength = exactPrefixLength;
		}

		@Override
		public void analyzer(String analyzerName) {
			this.overrideAnalyzerOrNormalizer = analysisDefinitionRegistry.getAnalyzerDefinition( analyzerName );
			if ( overrideAnalyzerOrNormalizer == null ) {
				throw AnalysisLog.INSTANCE.unknownAnalyzer( analyzerName, field.eventContext() );
			}
		}

		@Override
		public void skipAnalysis() {
			this.overrideAnalyzerOrNormalizer = AnalyzerConstants.KEYWORD_ANALYZER;
		}

		@Override
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			minimumShouldMatchConstraints.minimumShouldMatchNumber( ignoreConstraintCeiling, matchingClausesNumber );
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			minimumShouldMatchConstraints.minimumShouldMatchPercent( ignoreConstraintCeiling, matchingClausesPercent );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextMatchPredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			Analyzer effectiveAnalyzerOrNormalizer = overrideAnalyzerOrNormalizer;
			if ( effectiveAnalyzerOrNormalizer == null ) {
				effectiveAnalyzerOrNormalizer = field.type().searchAnalyzerOrNormalizer();
			}

			if ( effectiveAnalyzerOrNormalizer == AnalyzerConstants.KEYWORD_ANALYZER ) {
				// Optimization when analysis is disabled
				Term term = new Term( absoluteFieldPath, value );

				if ( maxEditDistance != null ) {
					return new FuzzyQuery( term, maxEditDistance, prefixLength );
				}
				else {
					return new TermQuery( term );
				}
			}

			QueryBuilder effectiveQueryBuilder;
			if ( maxEditDistance != null ) {
				effectiveQueryBuilder = new FuzzyQueryBuilder( effectiveAnalyzerOrNormalizer, maxEditDistance, prefixLength );
			}
			else {
				effectiveQueryBuilder = new QueryBuilder( effectiveAnalyzerOrNormalizer );
			}

			Query analyzed = effectiveQueryBuilder.createBooleanQuery( absoluteFieldPath, value );
			if ( analyzed == null ) {
				// Either the value was an empty string
				// or the analysis removed all tokens (that can happen if the value contained only stopwords, for example)
				// In any case, use the same behavior as Elasticsearch: don't match anything
				analyzed = new MatchNoDocsQuery( "No tokens after analysis of the value to match" );
			}
			return minimumShouldMatchConstraints.apply( analyzed );
		}
	}
}
