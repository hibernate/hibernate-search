/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchIndexSettingsBuilder;

/**
 * An Elasticsearch analyzer for which no definition was found in the Hibernate Search mapping.
 * <p>
 * Such an analyzer is expected to be defined separately on Elasticsearch.
 * <p>
 * This implementation is used whenever {@code @Analyzer(definition = "foo")} is encountered
 * and <strong>no</strong> {@code @AnalyzerDefinition} exists with the given name
 * ("foo" in this example).
 *
 * @author Yoann Rodiere
 */
public class UndefinedElasticsearchAnalyzerImpl implements ElasticsearchAnalyzer {

	private final String remoteName;

	public UndefinedElasticsearchAnalyzerImpl(String remoteName) {
		this.remoteName = remoteName;
	}

	@Override
	public String getName(String fieldName) {
		return remoteName;
	}

	@Override
	public String registerDefinitions(ElasticsearchIndexSettingsBuilder settingsBuilder, String fieldName) {
		// Nothing to do; we expect the analyzer to be already defined on the server.
		return remoteName;
	}

	@Override
	public void close() {
		// nothing to do
	}

	@Override
	public int hashCode() {
		return remoteName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof UndefinedElasticsearchAnalyzerImpl ) {
			UndefinedElasticsearchAnalyzerImpl other = (UndefinedElasticsearchAnalyzerImpl) obj;
			return other.remoteName.equals( remoteName );
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( remoteName );
		sb.append( ">" );
		return sb.toString();
	}

}
