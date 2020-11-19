/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;

import com.google.gson.JsonObject;

public class Elasticsearch77TestDialect extends Elasticsearch78TestDialect {

	@Override
	public ElasticsearchRequest createTemplatePutRequest(String templateName, String pattern, int priority,
			JsonObject settings) {
		return createLegacyTemplatePutRequest( templateName, pattern, priority, settings );
	}

	@Override
	public ElasticsearchRequest createTemplateDeleteRequest(String templateName) {
		return createLegacyTemplateDeleteRequest( templateName );
	}

}
