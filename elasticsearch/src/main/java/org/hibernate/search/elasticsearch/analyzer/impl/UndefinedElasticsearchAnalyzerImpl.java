/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.annotations.AnalyzerDef;

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
	public void close() {
		// nothing to do
	}

	@Override
	public String getName(String fieldName) {
		return remoteName;
	}

	@Override
	public AnalyzerDef getDefinition(String fieldName) {
		// No definition
		return null;
	}

}
