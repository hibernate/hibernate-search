/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;

/**
 * Offers access to the execution context:
 * <ul>
 * <li>objects supporting the execution, such as the Elasticsearch client
 * <li>mutable data relating to the execution, such as buffered
 * index monitors or the list of dirty indexes.
 * </ul>
 * <p>
 * Implementation may not be thread-safe.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchWorkExecutionContext {

	ElasticsearchClient getClient();

	GsonProvider getGsonProvider();

	void setIndexDirty(URLEncodedString indexName);

	IndexingMonitor getBufferedIndexingMonitor(IndexingMonitor indexingMonitor);

}
