/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;

/**
 * A registry of analysis-related definitions for Lucene.
 *
 * @author Yoann Rodiere
 */
public final class LuceneAnalysisDefinitionRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, Analyzer> analyzerDefinitions = new TreeMap<>();

	private final Map<String, Analyzer> normalizerDefinitions = new TreeMap<>();

	public LuceneAnalysisDefinitionRegistry(LuceneAnalysisDefinitionContributor contributor) {
		contributor.contribute( new LuceneAnalysisDefinitionCollector() {
			@Override
			public void collectAnalyzer(String name, Analyzer analyzer) {
				Analyzer previous = analyzerDefinitions.putIfAbsent( name, analyzer );
				if ( previous != null && previous != analyzer ) {
					throw log.analyzerDefinitionNamingConflict( name );
				}
			}

			@Override
			public void collectNormalizer(String name, Analyzer normalizer) {
				Analyzer previous = normalizerDefinitions.putIfAbsent( name, normalizer );
				if ( previous != null && previous != normalizer ) {
					throw log.normalizerDefinitionNamingConflict( name );
				}
			}
		} );
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

}
