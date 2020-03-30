/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.spi;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;

/**
 * Creates the Elasticsearch client.
 *
 */
public interface ElasticsearchClientFactory {

	ElasticsearchClientImplementor create(ConfigurationPropertySource propertySource,
			ThreadProvider threadProvider, String threadNamePrefix,
			ScheduledExecutorService timeoutExecutorService,
			GsonProvider gsonProvider);

}
