/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.FuzzyQueryBuilder;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

public class LuceneTextMatchPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneTextMatchPredicate(Builder<?> builder) {
		super( builder );
	}

	public static class Factory<F>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<MatchPredicateBuilder, F, LuceneStandardFieldCodec<F, String>> {
		public Factory(LuceneStandardFieldCodec<F, String> codec) {
			super( codec );
		}

		@Override
		public Builder<F> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements MatchPredicateBuilder {
		private final LuceneStandardFieldCodec<F, String> codec;
		private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

		private String value;

		private Integer maxEditDistance;
		private Integer prefixLength;

		private Analyzer overrideAnalyzerOrNormalizer;

		private Builder(LuceneStandardFieldCodec<F, String> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
			this.analysisDefinitionRegistry = scope.analysisDefinitionRegistry();
		}

		@Override
		public void value(Object value, ValueConvert convert) {
			this.value = convertAndEncode( codec, value, convert );
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
				throw log.unknownAnalyzer( analyzerName, field.eventContext() );
			}
		}

		@Override
		public void skipAnalysis() {
			this.overrideAnalyzerOrNormalizer = AnalyzerConstants.KEYWORD_ANALYZER;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextMatchPredicate( this );
		}

		@Override
		protected Query buildQuery() {
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
			return analyzed;
		}
	}
}
