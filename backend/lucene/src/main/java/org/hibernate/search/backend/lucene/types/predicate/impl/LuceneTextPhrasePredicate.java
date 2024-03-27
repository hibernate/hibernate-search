/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

public class LuceneTextPhrasePredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneTextPhrasePredicate(Builder<?> builder) {
		super( builder );
	}

	public static class Factory<F>
			extends AbstractLuceneValueFieldSearchQueryElementFactory<PhrasePredicateBuilder, F> {
		@Override
		public Builder<F> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F>
			implements PhrasePredicateBuilder {
		private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

		private int slop;
		private String phrase;

		private Analyzer overrideAnalyzer;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.analysisDefinitionRegistry = scope.analysisDefinitionRegistry();
		}

		@Override
		public void slop(int slop) {
			this.slop = slop;
		}

		@Override
		public void phrase(String phrase) {
			this.phrase = phrase;
		}

		@Override
		public void analyzer(String analyzerName) {
			this.overrideAnalyzer = analysisDefinitionRegistry.getAnalyzerDefinition( analyzerName );
			if ( overrideAnalyzer == null ) {
				throw log.unknownAnalyzer( analyzerName, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
			}
		}

		@Override
		public void skipAnalysis() {
			this.overrideAnalyzer = AnalyzerConstants.KEYWORD_ANALYZER;
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextPhrasePredicate( this );
		}

		@Override
		protected Query buildQuery() {
			Analyzer effectiveAnalyzerOrNormalizer = overrideAnalyzer;
			if ( effectiveAnalyzerOrNormalizer == null ) {
				effectiveAnalyzerOrNormalizer = field.type().searchAnalyzerOrNormalizer();
			}

			if ( effectiveAnalyzerOrNormalizer == AnalyzerConstants.KEYWORD_ANALYZER ) {
				// Optimization when analysis is disabled
				return new TermQuery( new Term( absoluteFieldPath, phrase ) );
			}

			Query analyzed =
					new QueryBuilder( effectiveAnalyzerOrNormalizer ).createPhraseQuery( absoluteFieldPath, phrase, slop );
			if ( analyzed == null ) {
				// Either the value was an empty string
				// or the analysis removed all tokens (that can happen if the value contained only stopwords, for example)
				// In any case, use the same behavior as Elasticsearch: don't match anything
				analyzed = new MatchNoDocsQuery( "No tokens after analysis of the phrase to match" );
			}
			return analyzed;
		}
	}
}
