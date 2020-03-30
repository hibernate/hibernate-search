/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;

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
	 * The minimal required status of an index on startup, before Hibernate Search can start using it.
	 * <p>
	 * Expects an {@link IndexStatus} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS}.
	 */
	public static final String SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS = "schema_management.minimal_required_status";

	/**
	 * The timeout when waiting for the {@link #SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS required status}.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 60000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT}.
	 */
	public static final String SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT = "schema_management.minimal_required_status_wait_timeout";

	/**
	 * The prefix for indexing-related property keys.
	 */
	public static final String INDEXING_PREFIX = "indexing.";

	/**
	 * The number of indexing queues assigned to each index.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed to such integer value.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_QUEUE_COUNT}.
	 */
	public static final String INDEXING_QUEUE_COUNT = INDEXING_PREFIX + IndexingRadicals.QUEUE_COUNT;

	/**
	 * The size of indexing queues.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed to such integer value.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_QUEUE_SIZE}.
	 */
	public static final String INDEXING_QUEUE_SIZE = INDEXING_PREFIX + IndexingRadicals.QUEUE_SIZE;

	/**
	 * The maximum size of bulk requests created when processing indexing queues.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed to such integer value.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_MAX_BULK_SIZE}.
	 */
	public static final String INDEXING_MAX_BULK_SIZE = INDEXING_PREFIX + IndexingRadicals.MAX_BULK_SIZE;

	/**
	 * Configuration property keys for indexing, without the {@link #INDEXING_PREFIX prefix}.
	 */
	public static final class IndexingRadicals {

		private IndexingRadicals() {
		}

		public static final String QUEUE_COUNT = "queue_count";
		public static final String QUEUE_SIZE = "queue_size";
		public static final String MAX_BULK_SIZE = "max_bulk_size";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final IndexStatus SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS = IndexStatus.GREEN;
		public static final int SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT = 10_000;
		public static final int INDEXING_QUEUE_COUNT = 10;
		public static final int INDEXING_QUEUE_SIZE = 1000;
		public static final int INDEXING_MAX_BULK_SIZE = 100;
	}

}
