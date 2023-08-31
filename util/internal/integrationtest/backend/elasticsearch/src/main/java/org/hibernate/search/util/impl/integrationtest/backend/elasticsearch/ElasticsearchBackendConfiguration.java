/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

public class ElasticsearchBackendConfiguration extends BackendConfiguration {

	@Override
	public String toString() {
		return "elasticsearch";
	}

	@Override
	public Map<String, String> rawBackendProperties() {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put( "log.json_pretty_printing", "true" );
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( properties );
		if ( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS
				.equals( ElasticsearchTestDialect.getActualVersion().distribution() ) ) {
			// The distribution/version cannot be detected on Amazon OpenSearch Serverless
			properties.put( "version", ElasticsearchTestDialect.getActualVersion().toString() );
		}
		return properties;
	}

	@Override
	public boolean supportsExplicitPurge() {
		return !ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS
				.equals( ElasticsearchTestDialect.getActualVersion().distribution() );
	}

	@Override
	public boolean supportsExplicitRefresh() {
		return !ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS
				.equals( ElasticsearchTestDialect.getActualVersion().distribution() );
	}

}
