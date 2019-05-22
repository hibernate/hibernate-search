/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

/**
 * Configuration properties for Elasticsearch indexes.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.IndexSettings} for details.
 */
public final class ElasticsearchIndexSettings {

	private ElasticsearchIndexSettings() {
	}

	/**
	 * The index lifecycle strategy to use, i.e. how to automatically administrate the index on startup/shutdown.
	 * <p>
	 * Expects an {@link ElasticsearchIndexLifecycleStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#LIFECYCLE_STRATEGY}.
	 */
	public static final String LIFECYCLE_STRATEGY = "lifecycle.strategy";

	/**
	 * The minimal required status of the index on startup, before Hibernate Search can start using it.
	 * <p>
	 * Expects an {@link ElasticsearchIndexStatus} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#LIFECYCLE_MINIMAL_REQUIRED_STATUS}.
	 */
	public static final String LIFECYCLE_MINIMAL_REQUIRED_STATUS = "lifecycle.minimal_required_status";

	/**
	 * The timeout when waiting for the {@link #LIFECYCLE_MINIMAL_REQUIRED_STATUS required status}.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 60000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT}.
	 */
	public static final String LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT = "lifecycle.minimal_required_status_wait_timeout";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final ElasticsearchIndexLifecycleStrategyName LIFECYCLE_STRATEGY = ElasticsearchIndexLifecycleStrategyName.CREATE;
		public static final ElasticsearchIndexStatus LIFECYCLE_MINIMAL_REQUIRED_STATUS = ElasticsearchIndexStatus.GREEN;
		public static final int LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT = 10_000;
	}

}
