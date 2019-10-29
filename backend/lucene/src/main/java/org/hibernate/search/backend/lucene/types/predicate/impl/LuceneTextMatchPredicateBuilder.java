/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneStandardMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.util.impl.FuzzyQueryBuilder;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class LuceneTextMatchPredicateBuilder<F>
		extends AbstractLuceneStandardMatchPredicateBuilder<F, String, LuceneTextFieldCodec<F>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneCompatibilityChecker analyzerChecker;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private Integer maxEditDistance;
	private Integer prefixLength;

	private Analyzer analyzer;
	private boolean analyzerOverridden = false;

	LuceneTextMatchPredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, LuceneTextFieldCodec<F> codec,
			Analyzer analyzerOrNormalizer, LuceneCompatibilityChecker analyzerChecker) {
		super( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec );
		this.analyzer = analyzerOrNormalizer;
		this.analyzerChecker = analyzerChecker;
		this.analysisDefinitionRegistry = searchContext.getAnalysisDefinitionRegistry();
	}

	@Override
	public void fuzzy(int maxEditDistance, int exactPrefixLength) {
		this.maxEditDistance = maxEditDistance;
		this.prefixLength = exactPrefixLength;
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

		if ( analyzer != null ) {
			QueryBuilder effectiveQueryBuilder;
			if ( maxEditDistance != null ) {
				effectiveQueryBuilder = new FuzzyQueryBuilder( analyzer, maxEditDistance, prefixLength );
			}
			else {
				effectiveQueryBuilder = new QueryBuilder( analyzer );
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
		else {
			// we are in the case where we a have a normalizer here as the analyzer case has already been treated by
			// the queryBuilder case above
			Term term = new Term( absoluteFieldPath, codec.normalize( absoluteFieldPath, value ) );

			if ( maxEditDistance != null ) {
				return new FuzzyQuery( term, maxEditDistance, prefixLength );
			}
			else {
				return new TermQuery( term );
			}
		}
	}
}
