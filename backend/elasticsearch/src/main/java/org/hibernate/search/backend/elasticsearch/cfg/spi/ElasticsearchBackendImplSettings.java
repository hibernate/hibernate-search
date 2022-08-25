/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg.spi;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;

/**
 * Implementation-related settings, used for testing only.
 */
public final class ElasticsearchBackendImplSettings {

	private ElasticsearchBackendImplSettings() {
	}

	public static final String CLIENT_FACTORY = "client_factory";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final BeanReference<ElasticsearchClientFactory> CLIENT_FACTORY =
				ElasticsearchClientFactoryImpl.REFERENCE;
	}
}
