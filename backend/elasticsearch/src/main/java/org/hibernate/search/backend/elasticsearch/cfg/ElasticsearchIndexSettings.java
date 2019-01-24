/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

/**
 * Configuration properties for Elasticsearch indexes.
 */
public final class ElasticsearchIndexSettings {

	private ElasticsearchIndexSettings() {
	}

	public static final String LIFECYCLE_STRATEGY = "lifecycle.strategy";

	public static final String LIFECYCLE_REQUIRED_STATUS = "lifecycle.required_status";

	public static final String LIFECYCLE_REQUIRED_STATUS_WAIT_TIMEOUT = "lifecycle.required_status_wait_timeout";

	public static final String REFRESH_AFTER_WRITE = "refresh_after_write";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final ElasticsearchIndexLifecycleStrategyName LIFECYCLE_STRATEGY = ElasticsearchIndexLifecycleStrategyName.CREATE;
		public static final ElasticsearchIndexStatus LIFECYCLE_REQUIRED_STATUS = ElasticsearchIndexStatus.GREEN;
		public static final int LIFECYCLE_REQUIRED_STATUS_WAIT_TIMEOUT = 10_000;
		public static final boolean REFRESH_AFTER_WRITE = false;
	}

}
