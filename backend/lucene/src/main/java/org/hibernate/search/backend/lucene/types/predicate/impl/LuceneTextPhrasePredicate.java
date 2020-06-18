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
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
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

class LuceneTextPhrasePredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneTextPhrasePredicate(Builder builder) {
		super( builder );
	}

	static class Builder<F> extends AbstractBuilder<F>
			implements PhrasePredicateBuilder {
		private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

		private int slop;
		private String phrase;

		private Analyzer overrideAnalyzer;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
			super( searchContext, field );
			this.analysisDefinitionRegistry = searchContext.analysisDefinitionRegistry();
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

			Query analyzed = new QueryBuilder( effectiveAnalyzerOrNormalizer ).createPhraseQuery( absoluteFieldPath, phrase, slop );
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
