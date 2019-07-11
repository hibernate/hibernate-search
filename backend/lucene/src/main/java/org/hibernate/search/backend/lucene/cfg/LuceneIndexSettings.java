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
	 * The number of shards to create for the index,
	 * i.e. the number of "physical" indexes, each holding a part of the index data.
	 * <p>
	 * Expects a strictly positive Integer value, such as 4,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#SHARDING_NUMBER_OF_SHARDS}.
	 */
	public static final String SHARDING_NUMBER_OF_SHARDS = SHARDING_PREFIX + ShardingRadicals.NUMBER_OF_SHARDS;

	/**
	 * Configuration property keys for sharding, without the {@link #SHARDING_PREFIX prefix}.
	 */
	public static final class ShardingRadicals {

		private ShardingRadicals() {
		}

		public static final String NUMBER_OF_SHARDS = "number_of_shards";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final int SHARDING_NUMBER_OF_SHARDS = 1;
	}
}
