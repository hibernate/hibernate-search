/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchWorkExecutionContext {

	ElasticsearchClient getClient();

	void registerIndexToRefresh(URLEncodedString indexName);

	GsonProvider getGsonProvider();

}
