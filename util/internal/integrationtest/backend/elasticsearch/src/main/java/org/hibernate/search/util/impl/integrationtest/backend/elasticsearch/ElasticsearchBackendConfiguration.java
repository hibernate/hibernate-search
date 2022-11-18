/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

import org.junit.jupiter.api.extension.Extension;

public class ElasticsearchBackendConfiguration extends BackendConfiguration {

	protected final TestElasticsearchClient testElasticsearchClient = TestElasticsearchClient.create();

	@Override
	public String toString() {
		return "elasticsearch";
	}

	@Override
	public Optional<Extension> extension() {
		return Optional.of( testElasticsearchClient );
	}

	public TestElasticsearchClient testElasticsearchClient() {
		return testElasticsearchClient;
	}

	@Override
	public Map<String, String> rawBackendProperties() {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put( "log.json_pretty_printing", "true" );
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( properties );
		return properties;
	}
}
