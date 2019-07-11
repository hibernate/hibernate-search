/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cfg;

/**
 * Configuration properties for Lucene indexes.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.IndexSettings} for details.
 */
public final class LuceneIndexSettings {

	private LuceneIndexSettings() {
	}

	/**
	 * The prefix for sharding-related property keys.
	 */
	public static final String SHARDING_PREFIX = "sharding.";

	/**
	 * The sharding strategy, deciding the number of shards, their identifiers,
	 * and how to translate a routing key into a shard identifier.
	 * <p>
	 * Expects a String, such as "hash".
	 * See the reference documentation for a list of available values.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#SHARDING_STRATEGY} (no sharding).
	 */
	public static final String SHARDING_STRATEGY = SHARDING_PREFIX + ShardingRadicals.STRATEGY;

	/**
	 * The number of shards to create for the index,
	 * i.e. the number of "physical" indexes, each holding a part of the index data.
	 * <p>
	 * Only available for the "hash" sharding strategy.
	 * <p>
	 * Expects a strictly positive Integer value, such as 4,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * No default: this property must be set when using the "hash" sharding strategy.
	 */
	public static final String SHARDING_NUMBER_OF_SHARDS = SHARDING_PREFIX + ShardingRadicals.NUMBER_OF_SHARDS;

	/**
	 * Configuration property keys for sharding, without the {@link #SHARDING_PREFIX prefix}.
	 */
	public static final class ShardingRadicals {

		private ShardingRadicals() {
		}

		public static final String STRATEGY = "strategy";
		public static final String NUMBER_OF_SHARDS = "number_of_shards";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final String SHARDING_STRATEGY = "none";
	}
}
