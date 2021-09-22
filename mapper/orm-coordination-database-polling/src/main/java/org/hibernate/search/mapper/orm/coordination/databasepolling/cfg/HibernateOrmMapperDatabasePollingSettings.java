/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.cfg;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.CoordinationStrategyNames;

public final class HibernateOrmMapperDatabasePollingSettings {

	private HibernateOrmMapperDatabasePollingSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property
	 * when using the Hibernate ORM mapper.
	 */
	public static final String PREFIX = EngineSettings.PREFIX;

	/**
	 * Whether shards are static, i.e. configured explicitly for each node, with a fixed number of shards/nodes.
	 * <p>
	 * <strong>WARNING:</strong> This property must have the same value for all application nodes,
	 * and must never change unless all application nodes are stopped, then restarted.
	 * Failing that, some events may not be processed or may be processed twice or in the wrong order,
	 * resulting in errors and/or out-of-sync indexes.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@link CoordinationStrategyNames#DATABASE_POLLING}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_SHARDS_STATIC}.
	 */
	public static final String COORDINATION_SHARDS_STATIC = PREFIX + Radicals.COORDINATION_SHARDS_STATIC;

	/**
	 * The total number of shards across all application nodes.
	 * <p>
	 * <strong>WARNING:</strong> This property must have the same value for all application nodes,
	 * and must never change unless all application nodes are stopped, then restarted.
	 * Failing that, some events may not be processed or may be processed twice or in the wrong order,
	 * resulting in errors and/or out-of-sync indexes.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@link CoordinationStrategyNames#DATABASE_POLLING}
	 * and {@link #COORDINATION_SHARDS_STATIC} is {@code true}.
	 * <p>
	 * Expects an Integer value of at least {@code 2},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * No default: must be provided when static sharding is enabled.
	 */
	public static final String COORDINATION_SHARDS_TOTAL_COUNT = PREFIX + Radicals.COORDINATION_SHARDS_TOTAL_COUNT;

	/**
	 * The indices of shards assigned to this application node.
	 * <p>
	 * <strong>WARNING:</strong> shards must be uniquely assigned to one and only one application nodes.
	 * Failing that, some events may not be processed or may be processed twice or in the wrong order,
	 * resulting in errors and/or out-of-sync indexes.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#AUTOMATIC_INDEXING_STRATEGY} is
	 * {@link CoordinationStrategyNames#DATABASE_POLLING}
	 * and {@link #COORDINATION_SHARDS_STATIC} is {@code true}.
	 * <p>
	 * Expects a shard index, i.e. an Integer value between {@code 0} (inclusive) and the
	 * {@link #COORDINATION_SHARDS_TOTAL_COUNT total shard count} (exclusive),
	 * or a String that can be parsed into such shard index,
	 * or a String containing multiple such shard index strings separated by commas,
	 * or a {@code Collection<Integer>} containing such shard indices.
	 * <p>
	 * No default: must be provided when static sharding is enabled.
	 */
	public static final String COORDINATION_SHARDS_ASSIGNED = PREFIX + Radicals.COORDINATION_SHARDS_ASSIGNED;

	/**
	 * Whether the application will process entity change events.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@link CoordinationStrategyNames#DATABASE_POLLING}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_PROCESSORS_INDEXING_ENABLED}.
	 * <p>
	 * When processing is disabled, events will still be produced by this application node whenever an entity changes,
	 * but indexing will not happen on this application node
	 * and is assumed to happen on another node.
	 */
	public static final String COORDINATION_PROCESSORS_INDEXING_ENABLED =
			PREFIX + Radicals.COORDINATION_PROCESSORS_INDEXING_ENABLED;

	/**
	 * In the background indexing processor, how long to wait for another query to the outbox events table
	 * after a query didn't return any event, in milliseconds.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@link CoordinationStrategyNames#DATABASE_POLLING}.
	 * <p>
	 * Hibernate Search will wait that long before polling again if the last polling didn't return any event:
	 * <ul>
	 *   <li>High values mean higher latency between DB changes and indexing, but less stress on the database when there are no events to process.</li>
	 *   <li>Low values mean lower latency between DB changes and indexing, but more stress on the database when there are no events to process.</li>
	 * </ul>
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 1000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_PROCESSORS_INDEXING_POLLING_INTERVAL}.
	 */
	public static final String COORDINATION_PROCESSORS_INDEXING_POLLING_INTERVAL =
			PREFIX + Radicals.COORDINATION_PROCESSORS_INDEXING_POLLING_INTERVAL;

	/**
	 * In the background indexing processor, how many outbox events, at most, are processed in a single transaction.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@link CoordinationStrategyNames#DATABASE_POLLING}.
	 * <p>
	 * Expects a positive Integer value, such as {@code 50},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_PROCESSORS_INDEXING_BATCH_SIZE}.
	 */
	public static final String COORDINATION_PROCESSORS_INDEXING_BATCH_SIZE =
			PREFIX + Radicals.COORDINATION_PROCESSORS_INDEXING_BATCH_SIZE;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static final class Radicals {

		private Radicals() {
		}

		public static final String COORDINATION = "coordination";
		public static final String COORDINATION_PREFIX = COORDINATION + ".";
		public static final String COORDINATION_SHARDS_STATIC = COORDINATION_PREFIX + CoordinationRadicals.SHARDS_STATIC;
		public static final String COORDINATION_SHARDS_TOTAL_COUNT = COORDINATION_PREFIX + CoordinationRadicals.SHARDS_TOTAL_COUNT;
		public static final String COORDINATION_SHARDS_ASSIGNED = COORDINATION_PREFIX + CoordinationRadicals.SHARDS_ASSIGNED;
		public static final String COORDINATION_PROCESSORS_INDEXING_ENABLED = COORDINATION_PREFIX + CoordinationRadicals.PROCESSORS_INDEXING_ENABLED;
		public static final String COORDINATION_PROCESSORS_INDEXING_POLLING_INTERVAL = COORDINATION_PREFIX + CoordinationRadicals.PROCESSORS_INDEXING_POLLING_INTERVAL;
		public static final String COORDINATION_PROCESSORS_INDEXING_BATCH_SIZE = COORDINATION_PREFIX + CoordinationRadicals.PROCESSORS_INDEXING_BATCH_SIZE;
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix} + {@link Radicals#COORDINATION_PREFIX}.
	 */
	public static final class CoordinationRadicals {

		private CoordinationRadicals() {
		}

		public static final String SHARDS_STATIC = "shards.static";
		public static final String SHARDS_TOTAL_COUNT = "shards.total_count";
		public static final String SHARDS_ASSIGNED = "shards.assigned";
		public static final String PROCESSORS_PREFIX = "processors.";
		public static final String PROCESSORS_INDEXING_PREFIX = PROCESSORS_PREFIX + "indexing.";
		public static final String PROCESSORS_INDEXING_ENABLED = PROCESSORS_INDEXING_PREFIX + "enabled";
		public static final String PROCESSORS_INDEXING_POLLING_INTERVAL = PROCESSORS_INDEXING_PREFIX + "polling_interval";
		public static final String PROCESSORS_INDEXING_BATCH_SIZE = PROCESSORS_INDEXING_PREFIX + "batch_size";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean COORDINATION_SHARDS_STATIC = false;
		public static final boolean COORDINATION_PROCESSORS_INDEXING_ENABLED = true;
		public static final int COORDINATION_PROCESSORS_INDEXING_POLLING_INTERVAL = 100;
		public static final int COORDINATION_PROCESSORS_INDEXING_BATCH_SIZE = 50;
	}

}
