/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;

public class LuceneQueryStringPredicate extends LuceneCommonQueryStringPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneQueryStringPredicate(Builder builder) {
		super( builder );
	}

	public static class Builder extends LuceneCommonQueryStringPredicate.Builder implements QueryStringPredicateBuilder {

		private Boolean allowLeadingWildcard = true;
		private Boolean enablePositionIncrements = true;
		private Integer phraseSlop;
		private RewriteMethod rewriteMethod;
		private Integer rewriteN;

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void allowLeadingWildcard(boolean allowLeadingWildcard) {
			this.allowLeadingWildcard = allowLeadingWildcard;
		}

		@Override
		public void enablePositionIncrements(boolean enablePositionIncrements) {
			this.enablePositionIncrements = enablePositionIncrements;
		}

		@Override
		public void phraseSlop(Integer phraseSlop) {
			this.phraseSlop = phraseSlop;
		}

		@Override
		public void rewriteMethod(RewriteMethod rewriteMethod, Integer n) {
			this.rewriteMethod = rewriteMethod;
			this.rewriteN = n;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneQueryStringPredicate( this );
		}

		@Override
		protected SearchQueryElementTypeKey<LuceneCommonQueryStringPredicateBuilderFieldState> typeKey() {
			return LucenePredicateTypeKeys.QUERY_STRING;
		}

		@Override
		protected Query buildQuery() {
			if ( queryString == null || queryString.trim().isEmpty() ) {
				// empty string -- no docs, to match the behaviour of a simple query predicate
				return new MatchNoDocsQuery();
			}

			MultiFieldQueryParser queryParser = create( buildWeights(), buildAnalyzer() );

			queryParser.setDefaultOperator( toOperator( defaultOperator ) );
			if ( rewriteMethod != null ) {
				queryParser.setMultiTermRewriteMethod( toRewriteMethod( rewriteMethod, rewriteN ) );
			}
			if ( allowLeadingWildcard != null ) {
				queryParser.setAllowLeadingWildcard( allowLeadingWildcard );
			}

			if ( enablePositionIncrements != null ) {
				queryParser.setEnablePositionIncrements( enablePositionIncrements );
			}
			if ( phraseSlop != null ) {
				queryParser.setPhraseSlop( phraseSlop );
			}

			try {
				return applyMinimumShouldMatch( queryParser.parse( queryString ) );
			}
			catch (ParseException e) {
				throw log.queryStringParseException( queryString, e.getMessage(), e );
			}
		}

		private MultiFieldQueryParser create(Map<String, Float> weights, Analyzer analyzer) {
			String[] fields = weights.keySet().toArray( String[]::new );
			return new MultiFieldQueryParser( fields, analyzer, weights );
		}

		private MultiTermQuery.RewriteMethod toRewriteMethod(RewriteMethod rewriteMethod, Integer n) {
			switch ( rewriteMethod ) {
				case CONSTANT_SCORE:
					return MultiTermQuery.CONSTANT_SCORE_REWRITE;
				case CONSTANT_SCORE_BOOLEAN:
					return MultiTermQuery.CONSTANT_SCORE_BOOLEAN_REWRITE;
				case SCORING_BOOLEAN:
					return MultiTermQuery.SCORING_BOOLEAN_REWRITE;
				case TOP_TERMS_BLENDED_FREQS_N:
					return new MultiTermQuery.TopTermsBlendedFreqScoringRewrite( n );
				case TOP_TERMS_BOOST_N:
					return new MultiTermQuery.TopTermsBoostOnlyBooleanQueryRewrite( n );
				case TOP_TERMS_N:
					return new MultiTermQuery.TopTermsScoringBooleanQueryRewrite( n );
				default:
					throw new AssertionFailure( "Unknown rewrite: " + rewriteMethod );
			}
		}

		private static QueryParser.Operator toOperator(BooleanOperator operator) {
			switch ( operator ) {
				case AND:
					return QueryParser.Operator.AND;
				case OR:
					return QueryParser.Operator.OR;
				default:
					throw new AssertionFailure( "Unknown boolean operator: " + operator );
			}
		}

	}
}
