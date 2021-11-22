/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public final class HibernateOrmMapperOutboxPollingSettings {

	private HibernateOrmMapperOutboxPollingSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property
	 * when using the Hibernate ORM mapper.
	 */
	public static final String PREFIX = HibernateOrmMapperSettings.PREFIX;

	/**
	 * The name of the outbox polling strategy,
	 * to be set on the {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY}
	 * configuration property in order to use this strategy.
	 * <p>
	 * With outbox polling, one or multiple application nodes exist,
	 * and they coordinate with each other by pushing data to additional tables in the database
	 * and polling these tables.
	 * <p>
	 * See the reference documentation for a comparison of all available coordination strategies
	 * and possible architectures.
	 */
	public static final String COORDINATION_STRATEGY_NAME = "outbox-polling";

	/**
	 * Whether shards are static, i.e. configured explicitly for each node, with a fixed number of shards/nodes.
	 * <p>
	 * <strong>WARNING:</strong> This property must have the same value for all application nodes,
	 * and must never change unless all application nodes are stopped, then restarted.
	 * Failing that, some events may not be processed or may be processed twice or in the wrong order,
	 * resulting in errors and/or out-of-sync indexes.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
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
	 * {@value #COORDINATION_STRATEGY_NAME}
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
	 * {@value #COORDINATION_STRATEGY_NAME}
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
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_ENABLED}.
	 * <p>
	 * When the event processor is disabled, events will still be produced by this application node whenever an entity changes,
	 * but indexing will not happen on this application node
	 * and is assumed to happen on another node.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_ENABLED =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_ENABLED;

	/**
	 * In the event processor, how long to wait for another query to the outbox events table
	 * after a query didn't return any event, in milliseconds.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
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
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL}.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL;

	/**
	 * How long, in milliseconds, the event processor can poll for events
	 * before it must perform a "pulse".
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Every agent registers itself in a database table.
	 * Regularly, while polling for events to process,
	 * the event processor performs a "pulse":
	 * it pauses indexing and:
	 * <ul>
	 *     <li>Updates its entry in the table, to let other agents know it's still alive and prevent an expiration</li>
	 *     <li>Removes any other agents that expired from the table</li>
	 *     <li>Performs rebalancing (reassignment of shards) if the number of agents
	 *     participating in background indexing changed since the last pulse</li>
	 * </ul>
	 * <p>
	 * The pulse interval must be set to a value between the
	 * {@link #COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL polling interval}
	 * and one third (1/3) of the {@link #COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION expiration interval}:
	 * <ul>
	 *   <li>Low values (closer to the polling interval) mean a shorter delay before rebalancing
	 *   when a node joins or leaves the cluster,
	 *   and reduced risk of incorrectly considering an agent as expired,
	 *   but more stress on the database because of more frequent checks of the list of agents.</li>
	 *   <li>High values (closer to the expiration interval) mean a longer delay before rebalancing
	 *   when a node joins or leaves the cluster,
	 *   and increased risk of incorrectly considering an agent as expired,
	 *   but less stress on the database because of less frequent checks of the list of agents.</li>
	 * </ul>
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 2000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL}.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL;

	/**
	 * How long, in milliseconds, an event processor "pulse" remains valid
	 * before considering the agent disconnected and forcibly removing it from the cluster.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Every agent registers itself in a database table.
	 * Regularly, while polling for events to process,
	 * the event processor performs a {@link #COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL "pulse"}:
	 * it pauses indexing and (among other things) update its entry in the table,
	 * to let other agents know it's still alive and prevent an expiration.
	 * If an agent fails to update its entry for longer than the value of the expiration interval,
	 * it will be considered disconnected: other agents will forcibly remove its entry from the table,
	 * and will perform rebalancing (reassign shards) as necessary.
	 * <p>
	 * The expiration interval must be set to a value 3 times larger than the
	 * {@link #COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL pulse interval}:
	 * <ul>
	 *   <li>Low values (closer to the pulse interval) mean a shorter delay before rebalancing
	 *   when a node abruptly leaves the cluster due to a crash or network failure,
	 *   but increased risk of incorrectly considering an agent as expired.</li>
	 *   <li>High values (much larger than the pulse interval) mean a longer delay before rebalancing
	 *   when a node abruptly leaves the cluster due to a crash or network failure,
	 *   but reduced risk of incorrectly considering an agent as expired.</li>
	 * </ul>
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 30000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION}.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION;

	/**
	 * In the event processor, how many outbox events, at most, are processed in a single transaction.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects a positive Integer value, such as {@code 50},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_BATCH_SIZE}.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_BATCH_SIZE =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_BATCH_SIZE;

	/**
	 * In the event processor, the timeout for transactions processing outbox events.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Only effective when a JTA transaction manager is configured.
	 * <p>
	 * Expects a positive Integer value in seconds, such as {@code 10},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * When using JTA and this property is not set,
	 * Hibernate Search will use whatever default transaction timeout is configured in the JTA transaction manager.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_TRANSACTION_TIMEOUT =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_TRANSACTION_TIMEOUT;

	/**
	 * In the event processor,
	 * the time after which it is possible to process again an event that has gone into error.
	 * <p>
	 * Only available when {@link HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects a positive integer value in seconds, such as {@code 10},
	 * or a String that can be parsed into such Integer value.
	 * Use the value {@code 0} to reprocess the failed events as soon as possible, with no delay.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_RETRY_DELAY}.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_RETRY_DELAY =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_RETRY_DELAY;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static final class Radicals {

		private Radicals() {
		}

		public static final String COORDINATION_PREFIX = HibernateOrmMapperSettings.Radicals.COORDINATION_PREFIX;
		public static final String COORDINATION_SHARDS_STATIC = COORDINATION_PREFIX + CoordinationRadicals.SHARDS_STATIC;
		public static final String COORDINATION_SHARDS_TOTAL_COUNT = COORDINATION_PREFIX + CoordinationRadicals.SHARDS_TOTAL_COUNT;
		public static final String COORDINATION_SHARDS_ASSIGNED = COORDINATION_PREFIX + CoordinationRadicals.SHARDS_ASSIGNED;
		public static final String COORDINATION_EVENT_PROCESSOR_ENABLED = COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_ENABLED;
		public static final String COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL = COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_POLLING_INTERVAL;
		public static final String COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL = COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_PULSE_INTERVAL;
		public static final String COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION = COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_PULSE_EXPIRATION;
		public static final String COORDINATION_EVENT_PROCESSOR_BATCH_SIZE = COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_BATCH_SIZE;
		public static final String COORDINATION_EVENT_PROCESSOR_TRANSACTION_TIMEOUT = COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_TRANSACTION_TIMEOUT;
		public static final String COORDINATION_EVENT_PROCESSOR_RETRY_DELAY = COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_RETRY_DELAY;
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
		public static final String EVENT_PROCESSOR_PREFIX = "event_processor.";
		public static final String EVENT_PROCESSOR_ENABLED = EVENT_PROCESSOR_PREFIX + "enabled";
		public static final String EVENT_PROCESSOR_POLLING_INTERVAL = EVENT_PROCESSOR_PREFIX + "polling_interval";
		public static final String EVENT_PROCESSOR_PULSE_INTERVAL = EVENT_PROCESSOR_PREFIX + "pulse_interval";
		public static final String EVENT_PROCESSOR_PULSE_EXPIRATION = EVENT_PROCESSOR_PREFIX + "pulse_expiration";
		public static final String EVENT_PROCESSOR_BATCH_SIZE = EVENT_PROCESSOR_PREFIX + "batch_size";
		public static final String EVENT_PROCESSOR_TRANSACTION_TIMEOUT = EVENT_PROCESSOR_PREFIX + "transaction_timeout";
		public static final String EVENT_PROCESSOR_RETRY_DELAY = EVENT_PROCESSOR_PREFIX + "retry_delay";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean COORDINATION_SHARDS_STATIC = false;
		public static final boolean COORDINATION_EVENT_PROCESSOR_ENABLED = true;
		public static final int COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL = 100;
		public static final int COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL = 2000;
		public static final int COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION = 30000;
		public static final int COORDINATION_EVENT_PROCESSOR_BATCH_SIZE = 50;
		public static final int COORDINATION_EVENT_PROCESSOR_RETRY_DELAY = 30;
	}

}
