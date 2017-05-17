/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A simple implementation of {@link LuceneAnalysisDefinitionRegistry}.
 * <p>
 * This class also provides access to the full  mapping from names to definitions
 * (see {@link #getAnalyzerDefinitions} for instance)
 *
 * @author Yoann Rodiere
 */
public class SimpleLuceneAnalysisDefinitionRegistry implements LuceneAnalysisDefinitionRegistry {

	private static final Log log = LoggerFactory.make();

	private final Map<String, AnalyzerDef> analyzerDefinitions = new TreeMap<>();

	public SimpleLuceneAnalysisDefinitionRegistry() {
	}

	public SimpleLuceneAnalysisDefinitionRegistry(Map<String, AnalyzerDef> analyzerDefinitions) {
		this.analyzerDefinitions.putAll( analyzerDefinitions );
	}

	@Override
	public void register(String name, AnalyzerDef definition) {
		AnalyzerDef previous = analyzerDefinitions.putIfAbsent( name, definition );
		if ( previous != null && previous != definition ) {
			throw log.analyzerDefinitionNamingConflict( name );
		}
	}

	@Override
	public AnalyzerDef getAnalyzerDefinition(String name) {
		return analyzerDefinitions.get( name );
	}


	public Map<String, AnalyzerDef> getAnalyzerDefinitions() {
		return Collections.unmodifiableMap( analyzerDefinitions );
	}

}
