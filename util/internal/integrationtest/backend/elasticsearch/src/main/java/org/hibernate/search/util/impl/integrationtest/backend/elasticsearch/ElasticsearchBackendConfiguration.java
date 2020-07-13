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

import org.junit.rules.TestRule;

public class ElasticsearchBackendConfiguration extends BackendConfiguration {

	protected final TestElasticsearchClient testElasticsearchClient = new TestElasticsearchClient();

	@Override
	public String toString() {
		return "elasticsearch";
	}

	@Override
	public Optional<TestRule> testRule() {
		return Optional.of( testElasticsearchClient );
	}

	@Override
	protected Map<String, Object> backendProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put( "type", "elasticsearch" );
		properties.put( "log.json_pretty_printing", "true" );
		properties.put( "index_defaults.schema_management.minimal_required_status", "yellow" );
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( properties );
		return properties;
	}
}
