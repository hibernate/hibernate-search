/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionRegistryBuilder;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class LuceneAnalyzerDefinitionRegistryBuilderImpl implements LuceneAnalyzerDefinitionRegistryBuilder {

	private static final Log LOG = LoggerFactory.make();

	private Map<String, LuceneAnalyzerDefinitionContextImpl> definitions = new LinkedHashMap<>();

	@Override
	public LuceneAnalyzerDefinitionContext analyzer(String name) {
		LuceneAnalyzerDefinitionContextImpl definition = new LuceneAnalyzerDefinitionContextImpl( this, name );
		LuceneAnalyzerDefinitionContextImpl existing = definitions.putIfAbsent( name, definition );
		if ( existing != null ) {
			throw LOG.analyzerDefinitionNamingConflict( name );
		}
		return definition;
	}

	public SimpleLuceneAnalysisDefinitionRegistry build() {
		SimpleLuceneAnalysisDefinitionRegistry registry = new SimpleLuceneAnalysisDefinitionRegistry();
		for ( Map.Entry<String, LuceneAnalyzerDefinitionContextImpl> entry : definitions.entrySet() ) {
			registry.register( entry.getKey(), entry.getValue().build() );
		}
		return registry;
	}

}
