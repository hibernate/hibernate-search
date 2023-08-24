/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

public class LuceneBackendConfiguration extends BackendConfiguration {
	@Override
	public String toString() {
		return "lucene";
	}

	@Override
	public Map<String, String> rawBackendProperties() {
		Map<String, String> properties = new LinkedHashMap<>();
		properties.put(
				"directory.root",
				LuceneTestIndexesPathConfiguration.get().getPath()
						+ "/test-indexes/#{test.startup.timestamp}/#{test.id}/"
		);
		return properties;
	}

	@Override
	public boolean supportsExplicitPurge() {
		return true;
	}

	@Override
	public boolean supportsExplicitRefresh() {
		return true;
	}

}
