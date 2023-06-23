/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A registry of analysis-related definitions for Elasticsearch.
 * <p>
 * This class provides access to the full mapping from names to definitions
 * (see {@link #getAnalyzerDefinitions} for instance).
 *
 */
public final class ElasticsearchAnalysisDefinitionRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, AnalyzerDefinition> analyzerDefinitions;
	private final Map<String, NormalizerDefinition> normalizerDefinitions;
	private final Map<String, TokenizerDefinition> tokenizerDefinitions;
	private final Map<String, TokenFilterDefinition> tokenFilterDefinitions;
	private final Map<String, CharFilterDefinition> charFilterDefinitions;

	public ElasticsearchAnalysisDefinitionRegistry() {
		// Nothing to do: we're creating an empty registry
		analyzerDefinitions = Collections.emptyMap();
		normalizerDefinitions = Collections.emptyMap();
		tokenizerDefinitions = Collections.emptyMap();
		tokenFilterDefinitions = Collections.emptyMap();
		charFilterDefinitions = Collections.emptyMap();
	}

	public ElasticsearchAnalysisDefinitionRegistry(ElasticsearchAnalysisDefinitionContributor contributor) {
		analyzerDefinitions = new TreeMap<>();
		normalizerDefinitions = new TreeMap<>();
		tokenizerDefinitions = new TreeMap<>();
		tokenFilterDefinitions = new TreeMap<>();
		charFilterDefinitions = new TreeMap<>();
		contributor.contribute( new ElasticsearchAnalysisDefinitionCollector() {
			@Override
			public void collect(String name, AnalyzerDefinition definition) {
				// Override if existing
				analyzerDefinitions.put( name, definition );
			}

			@Override
			public void collect(String name, NormalizerDefinition definition) {
				// Override if existing
				normalizerDefinitions.put( name, definition );
			}

			@Override
			public void collect(String name, TokenizerDefinition definition) {
				TokenizerDefinition previous = tokenizerDefinitions.putIfAbsent( name, definition );
				if ( previous != null && previous != definition ) {
					throw log.tokenizerNamingConflict( name );
				}
			}

			@Override
			public void collect(String name, TokenFilterDefinition definition) {
				TokenFilterDefinition previous = tokenFilterDefinitions.putIfAbsent( name, definition );
				if ( previous != null && previous != definition ) {
					throw log.tokenFilterNamingConflict( name );
				}
			}

			@Override
			public void collect(String name, CharFilterDefinition definition) {
				CharFilterDefinition previous = charFilterDefinitions.putIfAbsent( name, definition );
				if ( previous != null && previous != definition ) {
					throw log.charFilterNamingConflict( name );
				}
			}
		} );
	}

	public Map<String, AnalyzerDefinition> getAnalyzerDefinitions() {
		return Collections.unmodifiableMap( analyzerDefinitions );
	}

	public Map<String, NormalizerDefinition> getNormalizerDefinitions() {
		return Collections.unmodifiableMap( normalizerDefinitions );
	}

	public Map<String, TokenizerDefinition> getTokenizerDefinitions() {
		return Collections.unmodifiableMap( tokenizerDefinitions );
	}

	public Map<String, TokenFilterDefinition> getTokenFilterDefinitions() {
		return Collections.unmodifiableMap( tokenFilterDefinitions );
	}

	public Map<String, CharFilterDefinition> getCharFilterDefinitions() {
		return Collections.unmodifiableMap( charFilterDefinitions );
	}

}
