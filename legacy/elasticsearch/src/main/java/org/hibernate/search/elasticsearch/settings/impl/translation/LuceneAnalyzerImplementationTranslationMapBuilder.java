/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility that helps {@link DefaultElasticsearchAnalyzerDefinitionTranslator}
 * build its analyzer implementation translation maps.
 *
 * @author Yoann Rodiere
 */
public class LuceneAnalyzerImplementationTranslationMapBuilder {

	private final Map<String, String> result = new HashMap<>();

	public LuceneAnalyzerImplementationTranslationMapBuilder() {
		super();
	}

	public LuceneAnalyzerImplementationTranslationMapBuilder add(Class<?> luceneClass, String elasticsearchName) {
		result.put( luceneClass.getName(), elasticsearchName );
		return this;
	}

	public Map<String, String> build() {
		return Collections.unmodifiableMap( result );
	}

}
