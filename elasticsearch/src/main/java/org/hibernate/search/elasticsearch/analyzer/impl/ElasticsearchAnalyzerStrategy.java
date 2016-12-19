/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerStrategy implements AnalyzerStrategy {

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
	public void initializeAnalyzerReferences(Collection<AnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		Map<String, ElasticsearchAnalyzer> initializedAnalyzers = new HashMap<>();
		for ( AnalyzerReference reference : references ) {
			if ( reference.is( NamedElasticsearchAnalyzerReference.class ) ) {
				NamedElasticsearchAnalyzerReference namedReference = reference.unwrap( NamedElasticsearchAnalyzerReference.class );
				initializeReference( initializedAnalyzers, namedReference, analyzerDefinitions );
			}
		}
	}

	private void initializeReference(Map<String, ElasticsearchAnalyzer> initializedAnalyzers,
			NamedElasticsearchAnalyzerReference analyzerReference, Map<String, AnalyzerDef> analyzerDefinitions) {
		String name = analyzerReference.getAnalyzerName();

		if ( analyzerReference.isInitialized() ) {
			initializedAnalyzers.put( name, analyzerReference.getAnalyzer() );
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
	public ScopedElasticsearchAnalyzerReference.Builder buildScopedAnalyzerReference(AnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedElasticsearchAnalyzerReference.Builder(
				initialGlobalAnalyzerReference.unwrap( ElasticsearchAnalyzerReference.class ),
				Collections.<String, ElasticsearchAnalyzerReference>emptyMap()
				);
	}
}
