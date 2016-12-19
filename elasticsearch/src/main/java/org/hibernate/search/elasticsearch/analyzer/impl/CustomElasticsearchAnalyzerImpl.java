/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.annotations.AnalyzerDef;

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
	public AnalyzerDef getDefinition(String fieldName) {
		return definition;
	}

	@Override
	public Class<?> getLuceneClass(String fieldName) {
		// No analyzer class
		return null;
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
		sb.append( definition );
		sb.append( ">" );
		return sb.toString();
	}

}
