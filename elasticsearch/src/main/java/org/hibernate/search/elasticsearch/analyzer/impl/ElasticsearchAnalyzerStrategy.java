/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerStrategy implements AnalyzerStrategy<ElasticsearchAnalyzerReference> {

	@Override
	public ElasticsearchAnalyzerReference createDefaultAnalyzerReference() {
		return new SimpleElasticsearchAnalyzerReference( new UndefinedElasticsearchAnalyzerImpl( "default" ) );
	}

	@Override
	public ElasticsearchAnalyzerReference createPassThroughAnalyzerReference() {
		return new SimpleElasticsearchAnalyzerReference( new UndefinedElasticsearchAnalyzerImpl( "keyword" ) );
	}

	@Override
	public NamedElasticsearchAnalyzerReference createNamedAnalyzerReference(String name) {
		return new NamedElasticsearchAnalyzerReference( name );
	}

	@Override
	public ElasticsearchAnalyzerReference createAnalyzerReference(Class<?> analyzerClass) {
		return new SimpleElasticsearchAnalyzerReference( new BuiltinElasticsearchAnalyzerImpl( analyzerClass ) );
	}

	@Override
	public void initializeNamedAnalyzerReferences(Map<String, ElasticsearchAnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		Map<String, ElasticsearchAnalyzer> initializedAnalyzers = new HashMap<>();
		for ( Map.Entry<String, ElasticsearchAnalyzerReference> entry : references.entrySet() ) {
			String name = entry.getKey();
			NamedElasticsearchAnalyzerReference namedReference = entry.getValue().unwrap( NamedElasticsearchAnalyzerReference.class );
			initializeReference( initializedAnalyzers, name, namedReference, analyzerDefinitions );
		}
	}

	private void initializeReference(Map<String, ElasticsearchAnalyzer> initializedAnalyzers, String name,
			NamedElasticsearchAnalyzerReference analyzerReference, Map<String, AnalyzerDef> analyzerDefinitions) {
		if ( analyzerReference.isInitialized() ) {
			initializedAnalyzers.put( analyzerReference.getAnalyzerName(), analyzerReference.getAnalyzer() );
			return;
		}

		ElasticsearchAnalyzer analyzer = initializedAnalyzers.get( name );

		if ( analyzer == null ) {
			AnalyzerDef analyzerDefinition = analyzerDefinitions.get( name );
			if ( analyzerDefinition == null ) {
				analyzer = new UndefinedElasticsearchAnalyzerImpl( name );
			}
			else {
				analyzer = new CustomElasticsearchAnalyzerImpl( analyzerDefinition );
			}
			initializedAnalyzers.put( name, analyzer );
		}

		analyzerReference.initialize( analyzer );
	}

	@Override
	public ScopedElasticsearchAnalyzerReference.Builder buildScopedAnalyzerReference(ElasticsearchAnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedElasticsearchAnalyzerReference.Builder(
				initialGlobalAnalyzerReference, Collections.<String, ElasticsearchAnalyzerReference>emptyMap()
				);
	}
}
