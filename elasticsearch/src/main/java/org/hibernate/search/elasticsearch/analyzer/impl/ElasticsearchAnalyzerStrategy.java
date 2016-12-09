/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.analyzer.impl.LazyRemoteAnalyzer;
import org.hibernate.search.analyzer.impl.RemoteAnalyzer;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerStrategy implements AnalyzerStrategy<RemoteAnalyzerReference> {

	@Override
	public RemoteAnalyzerReference createDefaultAnalyzerReference() {
		return new RemoteAnalyzerReference( new RemoteAnalyzer( "default" ) );
	}

	@Override
	public RemoteAnalyzerReference createPassThroughAnalyzerReference() {
		return new RemoteAnalyzerReference( new RemoteAnalyzer( "keyword" ) );
	}

	@Override
	public RemoteAnalyzerReference createAnalyzerReference(String name) {
		return new RemoteAnalyzerReference( new LazyRemoteAnalyzer( name ) );
	}

	@Override
	public RemoteAnalyzerReference createAnalyzerReference(Class<?> analyzerClass) {
		return null;
	}

	@Override
	public void initializeNamedAnalyzerReferences(Collection<RemoteAnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		Map<String, RemoteAnalyzer> initializedAnalyzers = new HashMap<>();
		for ( RemoteAnalyzerReference reference : references ) {
			initializeReference( initializedAnalyzers, reference, analyzerDefinitions );
		}
	}


	private void initializeReference(Map<String, RemoteAnalyzer> initializedAnalyzers, RemoteAnalyzerReference analyzerReference,
			Map<String, AnalyzerDef> analyzerDefinitions) {
		LazyRemoteAnalyzer lazyAnalyzer = (LazyRemoteAnalyzer) analyzerReference.getAnalyzer();

		String name = lazyAnalyzer.getName();
		RemoteAnalyzer delegate = initializedAnalyzers.get( name );

		if ( delegate == null ) {
			// TODO HSEARCH-2219 Actually use the definition
			delegate = buildAnalyzer( name );
			initializedAnalyzers.put( name, delegate );
		}

		lazyAnalyzer.setDelegate( delegate );
	}

	private RemoteAnalyzer buildAnalyzer(String name) {
		return new RemoteAnalyzer( name );
	}
}
