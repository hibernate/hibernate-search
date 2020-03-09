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
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

class LuceneTextPhrasePredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements PhrasePredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String absoluteFieldPath;
	protected final LuceneTextFieldCodec<?> codec;

	private final LuceneCompatibilityChecker analyzerChecker;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private int slop;
	private String phrase;

	private Analyzer analyzer;
	private boolean analyzerOverridden = false;

	LuceneTextPhrasePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath,
			LuceneTextFieldCodec<?> codec,
			Analyzer analyzerOrNormalizer, LuceneCompatibilityChecker analyzerChecker) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
		this.analyzer = analyzerOrNormalizer;
		this.analyzerChecker = analyzerChecker;
		this.analysisDefinitionRegistry = searchContext.getAnalysisDefinitionRegistry();
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
		this.analyzer = analysisDefinitionRegistry.getAnalyzerDefinition( analyzerName );
		if ( analyzer == null ) {
			throw log.unknownAnalyzer( analyzerName, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
		this.analyzerOverridden = true;
	}

	@Override
	public void skipAnalysis() {
		this.analyzer = AnalyzerConstants.KEYWORD_ANALYZER;
		this.analyzerOverridden = true;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		// in case of an overridden analyzer,
		// any analyzer incompatibility is overridden too
		if ( !this.analyzerOverridden ) {
			analyzerChecker.failIfNotCompatible();
		}

		if ( analyzer == AnalyzerConstants.KEYWORD_ANALYZER ) {
			// Optimization when analysis is disabled
			return new TermQuery( new Term( absoluteFieldPath, phrase ) );
		}

		Query analyzed = new QueryBuilder( analyzer ).createPhraseQuery( absoluteFieldPath, phrase, slop );
		if ( analyzed == null ) {
			// Either the value was an empty string
			// or the analysis removed all tokens (that can happen if the value contained only stopwords, for example)
			// In any case, use the same behavior as Elasticsearch: don't match anything
			analyzed = new MatchNoDocsQuery( "No tokens after analysis of the phrase to match" );
		}
		return analyzed;
	}
}
