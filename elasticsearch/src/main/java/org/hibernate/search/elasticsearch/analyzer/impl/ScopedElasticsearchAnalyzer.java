/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;

/**
 * A {@code ScopedElasticsearchAnalyzer} is a wrapper class containing all remote analyzers for a given class.
 *
 * {@code ScopedElasticsearchAnalyzer} behaves similar to {@code ElasticsearchAnalyzerImpl} but delegates requests for name
 * to the underlying {@code ElasticsearchAnalyzerImpl} depending on the requested field name.
 *
 * @author Guillaume Smet
 */
public class ScopedElasticsearchAnalyzer implements ElasticsearchAnalyzer {

	private final ElasticsearchAnalyzerReference globalAnalyzerReference;
	private final Map<String, ElasticsearchAnalyzerReference> scopedAnalyzerReferences;

	public ScopedElasticsearchAnalyzer(ElasticsearchAnalyzerReference globalAnalyzer) {
		this( globalAnalyzer, Collections.<String, ElasticsearchAnalyzerReference>emptyMap() );
	}

	// For package use only; assumes the map is immutable
	ScopedElasticsearchAnalyzer(ElasticsearchAnalyzerReference globalAnalyzerReference,
			Map<String, ElasticsearchAnalyzerReference> scopedAnalyzerReferences) {
		this.globalAnalyzerReference = globalAnalyzerReference;
		this.scopedAnalyzerReferences = scopedAnalyzerReferences;
	}

	private ElasticsearchAnalyzer getDelegate(String fieldName) {
		ElasticsearchAnalyzerReference analyzerReference = scopedAnalyzerReferences.get( fieldName );
		if ( analyzerReference == null ) {
			analyzerReference = globalAnalyzerReference;
		}
		return analyzerReference.getAnalyzer();
	}

	@Override
	public String getName(String fieldName) {
		return getDelegate( fieldName ).getName( fieldName );
	}

	@Override
	public String registerDefinitions(ElasticsearchAnalysisDefinitionRegistry registry, String fieldName) {
		return getDelegate( fieldName ).registerDefinitions( registry, fieldName );
	}

	@Override
	public void close() {
		// nothing to close
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( globalAnalyzerReference );
		sb.append( "," );
		sb.append( scopedAnalyzerReferences );
		sb.append( ">" );
		return sb.toString();
	}

}
