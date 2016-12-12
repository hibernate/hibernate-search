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
		return new ElasticsearchAnalyzerReference( new ElasticsearchAnalyzerImpl( "default" ) );
	}

	@Override
	public ElasticsearchAnalyzerReference createPassThroughAnalyzerReference() {
		return new ElasticsearchAnalyzerReference( new ElasticsearchAnalyzerImpl( "keyword" ) );
	}

	@Override
	public ElasticsearchAnalyzerReference createNamedAnalyzerReference(String name) {
		return new ElasticsearchAnalyzerReference( name );
	}

	@Override
	public ElasticsearchAnalyzerReference createAnalyzerReference(Class<?> analyzerClass) {
		return null;
	}

	@Override
	public void initializeNamedAnalyzerReferences(Map<String, ElasticsearchAnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		Map<String, ElasticsearchAnalyzer> initializedAnalyzers = new HashMap<>();
		for ( Map.Entry<String, ElasticsearchAnalyzerReference> entry : references.entrySet() ) {
			initializeReference( initializedAnalyzers, entry.getKey(), entry.getValue(), analyzerDefinitions );
		}
	}

	private void initializeReference(Map<String, ElasticsearchAnalyzer> initializedAnalyzers, String name,
			ElasticsearchAnalyzerReference analyzerReference, Map<String, AnalyzerDef> analyzerDefinitions) {
		if ( analyzerReference.isInitialized() ) {
			initializedAnalyzers.put( analyzerReference.getAnalyzerName(), analyzerReference.getAnalyzer() );
			return;
		}

		ElasticsearchAnalyzer analyzer = initializedAnalyzers.get( name );

		if ( analyzer == null ) {
			// TODO HSEARCH-2219 Actually use the definition
			analyzer = buildAnalyzer( name );
			initializedAnalyzers.put( name, analyzer );
		}

		analyzerReference.initialize( analyzer );
	}

	private ElasticsearchAnalyzer buildAnalyzer(String name) {
		return new ElasticsearchAnalyzerImpl( name );
	}

	@Override
	public ScopedElasticsearchAnalyzer.Builder buildScopedAnalyzer(ElasticsearchAnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedElasticsearchAnalyzer.Builder(
				initialGlobalAnalyzerReference, Collections.<String, ElasticsearchAnalyzerReference>emptyMap()
				);
	}
}
