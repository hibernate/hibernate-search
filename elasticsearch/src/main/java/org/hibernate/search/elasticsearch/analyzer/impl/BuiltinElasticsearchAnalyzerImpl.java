/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.annotations.AnalyzerDef;

/**
 * A builtin Elasticsearch analyzer referenced by its Lucene class.
 * <p>
 * The Lucene class is expected to be that of a core analyzer, not some custom implementation.
 *
 * @author Yoann Rodiere
 */
public class BuiltinElasticsearchAnalyzerImpl implements ElasticsearchAnalyzer {

	private final Class<?> luceneClass;

	public BuiltinElasticsearchAnalyzerImpl(Class<?> luceneClass) {
		this.luceneClass = luceneClass;
	}

	@Override
	public void close() {
		// nothing to do
	}

	@Override
	public String getName(String fieldName) {
		// No name
		return null;
	}

	@Override
	public AnalyzerDef getDefinition(String fieldName) {
		// No definition
		return null;
	}

	@Override
	public Class<?> getLuceneClass(String fieldName) {
		return luceneClass;
	}

}
