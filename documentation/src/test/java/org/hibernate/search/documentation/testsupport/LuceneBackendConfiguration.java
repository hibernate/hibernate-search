/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneTestIndexesPathConfiguration;

public class LuceneBackendConfiguration extends AbstractDocumentationBackendConfiguration {
	@Override
	public String toString() {
		return "lucene";
	}

	@Override
	protected Map<String, Object> getBackendProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put( "type", "lucene" );
		properties.put(
				"directory.root",
				LuceneTestIndexesPathConfiguration.get().getPath()
						+ "/test-indexes/#{tck.startup.timestamp}/#{tck.test.id}/"
		);
		properties.put(
				"analysis.configurer",
				new LuceneSimpleMappingAnalysisConfigurer()
		);
		return properties;
	}

}
