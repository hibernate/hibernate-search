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

import org.hibernate.search.analyzer.impl.RemoteAnalyzer;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerImpl;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.analyzer.impl.ScopedRemoteAnalyzer;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchAnalyzerStrategy implements AnalyzerStrategy<RemoteAnalyzerReference> {

	@Override
	public RemoteAnalyzerReference createDefaultAnalyzerReference() {
		return new RemoteAnalyzerReference( new RemoteAnalyzerImpl( "default" ) );
	}

	@Override
	public RemoteAnalyzerReference createPassThroughAnalyzerReference() {
		return new RemoteAnalyzerReference( new RemoteAnalyzerImpl( "keyword" ) );
	}

	@Override
	public RemoteAnalyzerReference createNamedAnalyzerReference(String name) {
		return new RemoteAnalyzerReference( name );
	}

	@Override
	public RemoteAnalyzerReference createAnalyzerReference(Class<?> analyzerClass) {
		return null;
	}

	@Override
	public void initializeNamedAnalyzerReferences(Map<String, RemoteAnalyzerReference> references, Map<String, AnalyzerDef> analyzerDefinitions) {
		Map<String, RemoteAnalyzer> initializedAnalyzers = new HashMap<>();
		for ( Map.Entry<String, RemoteAnalyzerReference> entry : references.entrySet() ) {
			initializeReference( initializedAnalyzers, entry.getKey(), entry.getValue(), analyzerDefinitions );
		}
	}

	private void initializeReference(Map<String, RemoteAnalyzer> initializedAnalyzers, String name,
			RemoteAnalyzerReference analyzerReference, Map<String, AnalyzerDef> analyzerDefinitions) {
		RemoteAnalyzer analyzer = initializedAnalyzers.get( name );

		if ( analyzer == null ) {
			// TODO HSEARCH-2219 Actually use the definition
			analyzer = buildAnalyzer( name );
			initializedAnalyzers.put( name, analyzer );
		}

		analyzerReference.initialize( analyzer );
	}

	private RemoteAnalyzer buildAnalyzer(String name) {
		return new RemoteAnalyzerImpl( name );
	}

	@Override
	public ScopedRemoteAnalyzer.Builder buildScopedAnalyzer(RemoteAnalyzerReference initialGlobalAnalyzerReference) {
		return new ScopedRemoteAnalyzer.Builder( initialGlobalAnalyzerReference, Collections.<String, RemoteAnalyzerReference>emptyMap() );
	}
}
