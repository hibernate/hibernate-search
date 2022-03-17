/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl.ElasticsearchSearchSyntax;
import org.hibernate.search.engine.search.common.spi.SearchIndexCompositeNodeContext;
import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public interface ElasticsearchSearchIndexScope<S extends ElasticsearchSearchIndexScope<?>>
		extends SearchIndexScope<S> {

	@Override
	ElasticsearchSearchIndexNodeContext child(SearchIndexCompositeNodeContext<?> parent, String name);

	ElasticsearchSearchIndexNodeContext field(String fieldPath);

	Gson userFacingGson();

	ElasticsearchSearchSyntax searchSyntax();

	DocumentIdHelper documentIdHelper();

	JsonObject filterOrNull(String tenantId);

	TimeoutManager createTimeoutManager(Long timeout, TimeUnit timeUnit, boolean exceptionOnTimeout);

	Collection<ElasticsearchSearchIndexContext> indexes();

	Map<String, ElasticsearchSearchIndexContext> mappedTypeNameToIndex();

	int maxResultWindow();
}
