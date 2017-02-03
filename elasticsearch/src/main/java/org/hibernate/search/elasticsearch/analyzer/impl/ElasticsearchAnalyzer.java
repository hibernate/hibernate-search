/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.impl.RemoteAnalyzer;

/**
 * A description of an Elasticsearch analyzer.
 *
 * @author Guillaume Smet
 */
public interface ElasticsearchAnalyzer extends RemoteAnalyzer {

	/**
	 * @param registry The registry analysis definitions should be registered to.
	 * @param fieldName The name of the field whose analyzer definitions should be registered.
	 * @return The name of the registered analyzer.
	 */
	String registerDefinitions(ElasticsearchAnalysisDefinitionRegistry registry, String fieldName);

}
