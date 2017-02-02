/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchIndexSettingsBuilder;

/**
 * A description of an Elasticsearch analyzer built through an analyzer definition.
 * <p>
 * This implementation is used whenever {@code @Analyzer(definition = "foo")} is encountered
 * and an {@code @AnalyzerDefinition} exists with the given name ("foo" in this example).
 *
 * @author Guillaume Smet
 * @author Yoann Rodiere
 */
public class CustomElasticsearchAnalyzerImpl implements ElasticsearchAnalyzer {

	private final AnalyzerDef definition;

	public CustomElasticsearchAnalyzerImpl(AnalyzerDef definition) {
		this.definition = definition;
	}

	@Override
	public String getName(String fieldName) {
		return definition.name();
	}

	@Override
	public String registerDefinitions(ElasticsearchIndexSettingsBuilder settingsBuilder, String fieldName) {
		return settingsBuilder.registerAnalyzer( definition );
	}

	@Override
	public void close() {
		// nothing to close
	}

	@Override
	public int hashCode() {
		return definition.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof CustomElasticsearchAnalyzerImpl ) {
			CustomElasticsearchAnalyzerImpl other = (CustomElasticsearchAnalyzerImpl) obj;
			return other.definition.equals( definition );
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "<" );
		sb.append( definition );
		sb.append( ">" );
		return sb.toString();
	}

}
