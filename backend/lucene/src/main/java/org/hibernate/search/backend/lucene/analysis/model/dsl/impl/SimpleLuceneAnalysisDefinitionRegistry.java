/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.AnalyzerDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.NormalizerDef;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionCollector;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionContributor;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A simple implementation of {@link LuceneAnalysisDefinitionRegistry}.
 * <p>
 * This class also provides access to the full  mapping from names to definitions
 * (see {@link #getAnalyzerDefinitions} for instance)
 *
 * @author Yoann Rodiere
 */
public class SimpleLuceneAnalysisDefinitionRegistry implements LuceneAnalysisDefinitionRegistry {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, AnalyzerDef> analyzerDefinitions = new TreeMap<>();

	private final Map<String, NormalizerDef> normalizerDefinitions = new TreeMap<>();

	public SimpleLuceneAnalysisDefinitionRegistry(LuceneAnalysisDefinitionContributor contributor) {
		contributor.contribute( new LuceneAnalysisDefinitionCollector() {
			@Override
			public void collect(String name, AnalyzerDef definition) {
				AnalyzerDef previous = analyzerDefinitions.putIfAbsent( name, definition );
				if ( previous != null && previous != definition ) {
					throw log.analyzerDefinitionNamingConflict( name );
				}
			}

			@Override
			public void collect(String name, NormalizerDef definition) {
				NormalizerDef previous = normalizerDefinitions.putIfAbsent( name, definition );
				if ( previous != null && previous != definition ) {
					throw log.normalizerDefinitionNamingConflict( name );
				}
			}
		} );
	}

	@Override
	public AnalyzerDef getAnalyzerDefinition(String name) {
		return analyzerDefinitions.get( name );
	}

	@Override
	public NormalizerDef getNormalizerDefinition(String name) {
		return normalizerDefinitions.get( name );
	}

	public Map<String, AnalyzerDef> getAnalyzerDefinitions() {
		return Collections.unmodifiableMap( analyzerDefinitions );
	}

	public Map<String, NormalizerDef> getNormalizerDefinitions() {
		return Collections.unmodifiableMap( normalizerDefinitions );
	}

}
