/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import org.hibernate.search.analyzer.impl.RemoteAnalyzer;
import org.hibernate.search.elasticsearch.settings.impl.ElasticsearchIndexSettingsBuilder;

/**
 * A description of an Elasticsearch analyzer.
 *
 * @author Guillaume Smet
 */
public interface ElasticsearchAnalyzer extends RemoteAnalyzer {

	/**
	 * @param settingsBuilder The builder to which analysis definitions should be registered.
	 * @param fieldName The name of the field whose analyzer definitions should be registered.
	 * @return The name of the registered analyzer.
	 */
	String registerDefinitions(ElasticsearchIndexSettingsBuilder settingsBuilder, String fieldName);

}
