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

import org.hibernate.search.analyzer.spi.ScopedAnalyzer;
import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchIndexSettingsBuilder;

/**
 * A {@code ScopedElasticsearchAnalyzer} is a wrapper class containing all remote analyzers for a given class.
 *
 * {@code ScopedElasticsearchAnalyzer} behaves similar to {@code ElasticsearchAnalyzerImpl} but delegates requests for name
 * to the underlying {@code ElasticsearchAnalyzerImpl} depending on the requested field name.
 *
 * @author Guillaume Smet
 */
public class ScopedElasticsearchAnalyzer implements ElasticsearchAnalyzer, ScopedAnalyzer {

	private final ElasticsearchAnalyzer globalAnalyzer;
	private final Map<String, ElasticsearchAnalyzer> scopedAnalyzers;

	public ScopedElasticsearchAnalyzer(ElasticsearchAnalyzer globalAnalyzer) {
		this( globalAnalyzer, Collections.<String, ElasticsearchAnalyzer>emptyMap() );
	}

	public ScopedElasticsearchAnalyzer(ElasticsearchAnalyzer globalAnalyzer,
			Map<String, ElasticsearchAnalyzer> scopedAnalyzers) {
		this.globalAnalyzer = globalAnalyzer;
		this.scopedAnalyzers = Collections.unmodifiableMap( new HashMap<>( scopedAnalyzers ) );
	}

	ElasticsearchAnalyzer getGlobalAnalyzer() {
		return globalAnalyzer;
	}

	Map<String, ElasticsearchAnalyzer> getScopedAnalyzers() {
		return scopedAnalyzers;
	}

	private ElasticsearchAnalyzer getDelegate(String fieldName) {
		ElasticsearchAnalyzer analyzer = scopedAnalyzers.get( fieldName );
		if ( analyzer == null ) {
			analyzer = globalAnalyzer;
		}
		return analyzer;
	}

	@Override
	public String getName(String fieldName) {
		return getDelegate( fieldName ).getName( fieldName );
	}

	@Override
	public String registerDefinitions(ElasticsearchIndexSettingsBuilder settingsBuilder, String fieldName) {
		return getDelegate( fieldName ).registerDefinitions( settingsBuilder, fieldName );
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
		sb.append( globalAnalyzer );
		sb.append( "," );
		sb.append( scopedAnalyzers );
		sb.append( ">" );
		return sb.toString();
	}

}
