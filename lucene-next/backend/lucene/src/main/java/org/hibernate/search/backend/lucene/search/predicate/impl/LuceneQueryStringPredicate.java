/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;

public class LuceneQueryStringPredicate extends LuceneCommonQueryStringPredicate {

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
		protected Query buildQuery(PredicateRequestContext context) {
			if ( queryString == null || queryString.trim().isEmpty() ) {
				// empty string -- no docs, to match the behaviour of a simple query predicate
				return MatchNoDocsQuery.INSTANCE;
			}

			MultiFieldQueryParser queryParser = create( buildWeights(), buildAnalyzer(), fieldStateLookup() );

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
				return addMatchAllForBoolMustNotOnly( minimumShouldMatchConstraints.apply( queryParser.parse( queryString ) ) );
			}
			catch (ParseException e) {
				throw QueryLog.INSTANCE.queryStringParseException( queryString, e.getMessage(), e );
			}
		}

		private MultiFieldQueryParser create(Map<String, Float> weights, Analyzer analyzer,
				Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStateMap) {
			return new HibernateSearchMultiFieldQueryParser( analyzer, weights, fieldStateMap, scope );
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

	private static class HibernateSearchMultiFieldQueryParser extends MultiFieldQueryParser {
		private final Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStates;
		private final LuceneSearchIndexScope<?> scope;

		public HibernateSearchMultiFieldQueryParser(Analyzer analyzer, Map<String, Float> boosts,
				Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStates, LuceneSearchIndexScope<?> scope) {
			super( boosts.keySet().toArray( String[]::new ), analyzer, boosts );
			this.fieldStates = fieldStates;
			this.scope = scope;
		}

		@Override
		protected Query newFieldQuery(Analyzer analyzer, String field, String queryText, boolean quoted)
				throws ParseException {
			var state = fieldStates.get( field );

			if ( !state.field().type().valueClass().isAssignableFrom( String.class ) ) {
				var builder = state.field().queryElement( PredicateTypeKeys.MATCH, scope );
				builder.value( queryText, ValueModel.STRING );

				return LuceneSearchPredicate.from( scope, builder.build() ).toQuery( contextForField( state ) );
			}

			return super.newFieldQuery( analyzer, field, queryText, quoted );
		}

		@Override
		protected Query getFuzzyQuery(String field, String termStr, float minSimilarity) throws ParseException {
			checkFieldsAreAcceptable( "Fuzzy", fieldStates );
			return super.getFuzzyQuery( field, termStr, minSimilarity );
		}

		@Override
		protected Query getPrefixQuery(String field, String termStr) throws ParseException {
			checkFieldsAreAcceptable( "Prefix", fieldStates );
			return super.getPrefixQuery( field, termStr );
		}

		@Override
		protected Query getRegexpQuery(String field, String termStr) throws ParseException {
			checkFieldsAreAcceptable( "Regexp", fieldStates );
			return super.getRegexpQuery( field, termStr );
		}

		@Override
		protected Query getWildcardQuery(String field, String termStr) throws ParseException {
			checkFieldsAreAcceptable( "Wildcard", fieldStates );
			return super.getWildcardQuery( field, termStr );
		}

		@Override
		protected Query newRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
			var state = fieldStates.get( field );

			if ( !state.field().type().valueClass().isAssignableFrom( String.class ) ) {
				var builder = state.field().queryElement( PredicateTypeKeys.RANGE, scope );
				builder.within(
						org.hibernate.search.util.common.data.Range.between(
								part1, startInclusive ? RangeBoundInclusion.INCLUDED : RangeBoundInclusion.EXCLUDED,
								part2, endInclusive ? RangeBoundInclusion.INCLUDED : RangeBoundInclusion.EXCLUDED
						),
						ValueModel.STRING, ValueModel.STRING );
				return LuceneSearchPredicate.from( scope, builder.build() ).toQuery( contextForField( state ) );
			}

			return super.newRangeQuery( field, part1, part2, startInclusive, endInclusive );
		}
	}
}
