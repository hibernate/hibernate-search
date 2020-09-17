/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.impl;

import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

/**
 * A registry of analysis-related definitions for Lucene.
 *
 */
public final class LuceneAnalysisDefinitionRegistry {

	private final Similarity similarity;

	private final Map<String, Analyzer> analyzerDefinitions;

	private final Map<String, Analyzer> normalizerDefinitions;

	public LuceneAnalysisDefinitionRegistry(LuceneAnalysisDefinitionContributor contributor) {
		similarity = contributor.getSimilarity().orElseGet( LuceneAnalysisDefinitionRegistry::createDefaultSimilarity );
		analyzerDefinitions = new TreeMap<>();
		normalizerDefinitions = new TreeMap<>();
		contributor.contribute( new LuceneAnalysisDefinitionCollector() {
			@Override
			public void collectAnalyzer(String name, Analyzer analyzer) {
				// Override if existing
				analyzerDefinitions.put( name, analyzer );
			}

			@Override
			public void collectNormalizer(String name, Analyzer normalizer) {
				// Override if existing
				normalizerDefinitions.put( name, normalizer );
			}
		} );
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	/**
	 * @param name An analyzer name
	 * @return The analyzer definition associated with the given name,
	 * or {@code null} if there isn't any.
	 */
	public Analyzer getAnalyzerDefinition(String name) {
		return analyzerDefinitions.get( name );
	}

	/**
	 * @param name A normalizer name
	 * @return The normalizer definition associated with the given name,
	 * or {@code null} if there isn't any.
	 */
	public Analyzer getNormalizerDefinition(String name) {
		return normalizerDefinitions.get( name );
	}

	private static Similarity createDefaultSimilarity() {
		return new BM25Similarity();
	}
}
