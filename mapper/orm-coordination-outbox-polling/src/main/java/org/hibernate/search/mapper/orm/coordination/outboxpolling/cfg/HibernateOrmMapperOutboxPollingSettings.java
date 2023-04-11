/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cfg;

import static java.lang.String.join;

import org.hibernate.id.uuid.CustomVersionOneStrategy;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
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
	 * The root property for coordination properties specific to a tenant,
	 * e.g. "hibernate.search.coordination.tenants.tenant1.something = somevalue".
	 */
	public static final String COORDINATION_TENANTS = PREFIX + Radicals.COORDINATION_TENANTS;

	/**
	 * Whether the application will process entity change events.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed into a Boolean value.
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
	 * The total number of shards across all application nodes for event processing.
	 * <p>
	 * <strong>WARNING:</strong> This property must have the same value for all application nodes,
	 * and must never change unless all application nodes are stopped, then restarted.
	 * Failing that, some events may not be processed or may be processed twice or in the wrong order,
	 * resulting in errors and/or out-of-sync indexes.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * When this property is set, {@value #COORDINATION_EVENT_PROCESSOR_SHARDS_ASSIGNED} must also be set.
	 * <p>
	 * Expects an Integer value of at least {@code 2},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * No default: must be provided explicitly if you want static sharding.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT;

	/**
	 * The indices of shards assigned to this application node for event processing.
	 * <p>
	 * <strong>WARNING:</strong> shards must be uniquely assigned to one and only one application nodes.
	 * Failing that, some events may not be processed or may be processed twice or in the wrong order,
	 * resulting in errors and/or out-of-sync indexes.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * When this property is set, {@value #COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT} must also be set.
	 * <p>
	 * Expects a shard index, i.e. an Integer value between {@code 0} (inclusive) and the
	 * {@link #COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT total shard count} (exclusive),
	 * or a String that can be parsed into such shard index,
	 * or a String containing multiple such shard index strings separated by commas,
	 * or a {@code Collection<Integer>} containing such shard indices.
	 * <p>
	 * No default: must be provided explicitly if you want static sharding.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_SHARDS_ASSIGNED =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_SHARDS_ASSIGNED;

	/**
	 * In the event processor, how long to wait for another query to the outbox events table
	 * after a query didn't return any event, in milliseconds.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Hibernate Search will wait that long before polling again if the last polling didn't return any event:
	 * <ul>
	 *    <li>High values mean higher latency between an entity change and the corresponding update in the index,
	 *    but less stress on the database when there are no events to process.</li>
	 *    <li>Low values mean lower latency between an entity change and the corresponding update in the index,
	 *    but more stress on the database when there are no events to process.</li>
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
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Every agent registers itself in a database table.
	 * Regularly, while polling for events to process,
	 * the event processor performs a "pulse":
	 * it pauses indexing and:
	 * <ul>
	 *     <li>It updates its own entry in the table, to let other agents know it's still alive and prevent an expiration</li>
	 *     <li>It removes any other agents that expired from the table</li>
	 *     <li>It suspends itself if it notices a mass indexer is running</li>
	 *     <li>It performs rebalancing (reassignment of shards) if the number of agents
	 *     participating in background indexing changed since the last pulse</li>
	 * </ul>
	 * <p>
	 * The pulse interval must be set to a value between the
	 * {@link #COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL polling interval}
	 * and one third (1/3) of the {@link #COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION expiration interval}:
	 * <ul>
	 *   <li>Low values (closer to the polling interval) mean less time wasted not processing events
	 *   when a node joins or leaves the cluster,
	 *   and reduced risk of wasting time not processing events
	 *   because an event processor is incorrectly considered disconnected,
	 *   but more stress on the database because of more frequent checks of the list of agents.</li>
	 *   <li>High values (closer to the expiration interval) mean more time wasted not processing events
	 *   when a node joins or leaves the cluster,
	 *   and increased risk of wasting time not processing events
	 *   because an event processor is incorrectly considered disconnected,
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
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Every agent registers itself in a database table.
	 * Regularly, while polling for events to process,
	 * mass indexer agent performs a {@link #COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL "pulse"}:
	 * it pauses what it was doing and (among other things) updates its entry in the table,
	 * to let other agents know it's still alive and prevent an expiration.
	 * If an agent fails to update its entry for longer than the value of the expiration interval,
	 * it will be considered disconnected: other agents will forcibly remove its entry from the table,
	 * and will perform rebalancing (reassign shards) as necessary.
	 * <p>
	 * The expiration interval must be set to a value at least 3 times larger than the
	 * {@link #COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL pulse interval}:
	 * <ul>
	 *   <li>Low values (closer to the pulse interval) mean less time wasted not processing events
	 *   when a node abruptly leaves the cluster due to a crash or network failure,
	 *   but increased risk of wasting time not processing events
	 *   because an event processor is incorrectly considered disconnected.</li>
	 *   <li>High values (much larger than the pulse interval) mean more time wasted not processing events
	 *   when a node abruptly leaves the cluster due to a crash or network failure,
	 *   but reduced risk of wasting time not processing events
	 *   because an event processor is incorrectly considered disconnected.</li>
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
	 * In the event processor, the order in which outbox events are processed.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects one of the "external representation" strings defined in {@link OutboxEventProcessingOrder}.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_ORDER}.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_ORDER =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_ORDER;

	/**
	 * In the event processor, how many outbox events, at most, are processed in a single transaction.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
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
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
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
	 * How long the event processor must wait before re-processing an event after its processing failed.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects a positive integer value in seconds, such as {@code 10},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Use the value {@code 0} to reprocess the failed events as soon as possible, with no delay.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_EVENT_PROCESSOR_RETRY_DELAY}.
	 */
	public static final String COORDINATION_EVENT_PROCESSOR_RETRY_DELAY =
			PREFIX + Radicals.COORDINATION_EVENT_PROCESSOR_RETRY_DELAY;

	/**
	 * In the mass indexer, how long to wait for another query to the agent table
	 * when actively waiting for event processors to suspend themselves, in milliseconds.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Hibernate Search will wait that long before polling again when it finds other agents haven't suspended yet:
	 * <ul>
	 *   <li>Low values will reduce the time it takes for the mass indexer agent to detect that event processors finally suspended themselves,
	 *   but will increase the stress on the database while the mass indexer agent is actively waiting.</li>
	 *   <li>High values will increase the time it takes for the mass indexer agent to detect that event processors finally suspended themselves,
	 *   but will reduce the stress on the database while the mass indexer agent is actively waiting.</li>
	 * </ul>
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 1000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_MASS_INDEXER_POLLING_INTERVAL}.
	 */
	public static final String COORDINATION_MASS_INDEXER_POLLING_INTERVAL =
			PREFIX + Radicals.COORDINATION_MASS_INDEXER_POLLING_INTERVAL;

	/**
	 * How long, in milliseconds, the mass indexer can wait before it must perform a "pulse".
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Every agent registers itself in a database table.
	 * Regularly, the mass indexer performs a "pulse":
	 * <ul>
	 *     <li>It updates its owm entry in the table, to let other agents know it's still alive and prevent an expiration</li>
	 *     <li>It removes any other agents that expired from the table</li>
	 * </ul>
	 * <p>
	 * The pulse interval must be set to a value between the
	 * {@link #COORDINATION_MASS_INDEXER_POLLING_INTERVAL polling interval}
	 * and one third (1/3) of the {@link #COORDINATION_MASS_INDEXER_PULSE_EXPIRATION expiration interval}:
	 * <ul>
	 *   <li>Low values (closer to the polling interval) mean reduced risk of
	 *   event processors starting to process events again during mass indexing
	 *   because a mass indexer agent is incorrectly considered disconnected,
	 *   but more stress on the database because of more frequent updates of the mass indexer agent's entry in the agent table.</li>
	 *   <li>High values (closer to the expiration interval) mean increased risk of
	 *   event processors starting to process events again during mass indexing
	 *   because a mass indexer agent is incorrectly considered disconnected,
	 *   but less stress on the database because of less frequent updates of the mass indexer agent's entry in the agent table.</li>
	 * </ul>
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 2000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_MASS_INDEXER_PULSE_INTERVAL}.
	 */
	public static final String COORDINATION_MASS_INDEXER_PULSE_INTERVAL =
			PREFIX + Radicals.COORDINATION_MASS_INDEXER_PULSE_INTERVAL;

	/**
	 * How long, in milliseconds, a mass indexer agent "pulse" remains valid
	 * before considering the agent disconnected and forcibly removing it from the cluster.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value #COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Every agent registers itself in a database table.
	 * Regularly, while polling for events to process,
	 * each agent performs a {@link #COORDINATION_MASS_INDEXER_PULSE_INTERVAL "pulse"}:
	 * it pauses what it was doing and (among other things) updates its entry in the table,
	 * to let other agents know it's still alive and prevent an expiration.
	 * If an agent fails to update its entry for longer than the value of the expiration interval,
	 * it will be considered disconnected: other agents will forcibly remove its entry from the table,
	 * and will resume their work as if the expired agent didn't exist.
	 * <p>
	 * The expiration interval must be set to a value at least 3 times larger than the
	 * {@link #COORDINATION_MASS_INDEXER_PULSE_INTERVAL pulse interval}:
	 * <ul>
	 *   <li>Low values (closer to the pulse interval) mean less time wasted with event processors not processing events
	 *   when a mass indexer agent terminates due to a crash,
	 *   but increased risk of event processors starting to process events again during mass indexing
	 *   because a mass indexer agent is incorrectly considered disconnected.</li>
	 *   <li>High values (much larger than the pulse interval) mean more time wasted with event processors not processing events
	 *   when a mass indexer agent terminates due to a crash,
	 *   but reduced risk of event processors starting to process events again during mass indexing
	 *   because a mass indexer agent is incorrectly considered disconnected.</li>
	 * </ul>
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 30000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_MASS_INDEXER_PULSE_EXPIRATION}.
	 */
	public static final String COORDINATION_MASS_INDEXER_PULSE_EXPIRATION =
			PREFIX + Radicals.COORDINATION_MASS_INDEXER_PULSE_EXPIRATION;


	/**
	 * The database catalog to use for the outbox event table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Defaults to the default catalog configured in Hibernate ORM. See {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_CATALOG =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_CATALOG;

	/**
	 * The database schema to use for the outbox event table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Defaults to the default schema configured in Hibernate ORM. See {@link org.hibernate.cfg.AvailableSettings#DEFAULT_SCHEMA}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_SCHEMA =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_SCHEMA;

	/**
	 * The name of the outbox event table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * The default for this value is {@value Defaults#COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_TABLE =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_TABLE;

	/**
	 * The name of UUID generator strategy to be used by {@link UuidGenerator} for the outbox event table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * The default for this value is {@link  Defaults#COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_GEN_STRATEGY}.
	 *
	 * @see UuidGenerator
	 * @see org.hibernate.annotations.UuidGenerator
	 * @see CustomVersionOneStrategy
	 * @see StandardRandomStrategy
	 */
	public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY;

	/**
	 * The name of the {@link org.hibernate.type.BasicType Hibernate ORM registered basic type} used for representing an UUID in the outbox event table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * The default for this value is {@link  Defaults#COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_TYPE}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE;

	/**
	 * The database catalog to use for the agent table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Defaults to the default catalog configured in Hibernate ORM. See {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_AGENT_CATALOG =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_AGENT_CATALOG;

	/**
	 * The database schema to use for the agent table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Defaults to the default schema configured in Hibernate ORM. See {@link org.hibernate.cfg.AvailableSettings#DEFAULT_SCHEMA}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_AGENT_SCHEMA =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_AGENT_SCHEMA;

	/**
	 * The name of the agent table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * The default for this value is {@value Defaults#COORDINATION_ENTITY_MAPPING_AGENT_TABLE}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_AGENT_TABLE =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_AGENT_TABLE;

	/**
	 * The name of UUID generator strategy to be used by {@link UuidGenerator} for the agent table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * The default for this value is {@link  Defaults#COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY}.
	 *
	 * @see UuidGenerator
	 * @see CustomVersionOneStrategy
	 * @see StandardRandomStrategy
	 */
	public static final String COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY;

	/**
	 * The name of the {@link org.hibernate.type.BasicType Hibernate ORM registered basic type} used for representing an UUID in the agent table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * The default for this value is {@link  Defaults#COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE}.
	 */
	public static final String COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE =
			PREFIX + Radicals.COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static final class Radicals {

		private Radicals() {
		}

		public static final String COORDINATION_PREFIX = HibernateOrmMapperSettings.Radicals.COORDINATION_PREFIX;
		public static final String COORDINATION_TENANTS = COORDINATION_PREFIX + CoordinationRadicals.TENANTS;
		public static final String COORDINATION_EVENT_PROCESSOR_ENABLED =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_ENABLED;
		public static final String COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_SHARDS_TOTAL_COUNT;
		public static final String COORDINATION_EVENT_PROCESSOR_SHARDS_ASSIGNED =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_SHARDS_ASSIGNED;
		public static final String COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_POLLING_INTERVAL;
		public static final String COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_PULSE_INTERVAL;
		public static final String COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_PULSE_EXPIRATION;
		public static final String COORDINATION_EVENT_PROCESSOR_ORDER =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_ORDER;
		public static final String COORDINATION_EVENT_PROCESSOR_BATCH_SIZE =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_BATCH_SIZE;
		public static final String COORDINATION_EVENT_PROCESSOR_TRANSACTION_TIMEOUT =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_TRANSACTION_TIMEOUT;
		public static final String COORDINATION_EVENT_PROCESSOR_RETRY_DELAY =
				COORDINATION_PREFIX + CoordinationRadicals.EVENT_PROCESSOR_RETRY_DELAY;
		public static final String COORDINATION_MASS_INDEXER_POLLING_INTERVAL =
				COORDINATION_PREFIX + CoordinationRadicals.MASS_INDEXER_POLLING_INTERVAL;
		public static final String COORDINATION_MASS_INDEXER_PULSE_INTERVAL =
				COORDINATION_PREFIX + CoordinationRadicals.MASS_INDEXER_PULSE_INTERVAL;
		public static final String COORDINATION_MASS_INDEXER_PULSE_EXPIRATION =
				COORDINATION_PREFIX + CoordinationRadicals.MASS_INDEXER_PULSE_EXPIRATION;
		public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_CATALOG =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_CATALOG;
		public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_SCHEMA =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_SCHEMA;
		public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_TABLE =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_TABLE;
		public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY;
		public static final String COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE;
		public static final String COORDINATION_ENTITY_MAPPING_AGENT_CATALOG =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_AGENT_CATALOG;
		public static final String COORDINATION_ENTITY_MAPPING_AGENT_SCHEMA =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_AGENT_SCHEMA;
		public static final String COORDINATION_ENTITY_MAPPING_AGENT_TABLE =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_AGENT_TABLE;
		public static final String COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY;
		public static final String COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE =
				COORDINATION_PREFIX + CoordinationRadicals.ENTITY_MAPPING_AGENT_UUID_TYPE;
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix} + {@link Radicals#COORDINATION_PREFIX}.
	 */
	public static final class CoordinationRadicals {

		private CoordinationRadicals() {
		}

		public static final String TENANTS = "tenants";
		public static final String EVENT_PROCESSOR_PREFIX = "event_processor.";
		public static final String EVENT_PROCESSOR_ENABLED = EVENT_PROCESSOR_PREFIX + "enabled";
		public static final String EVENT_PROCESSOR_SHARDS_TOTAL_COUNT = EVENT_PROCESSOR_PREFIX + "shards.total_count";
		public static final String EVENT_PROCESSOR_SHARDS_ASSIGNED = EVENT_PROCESSOR_PREFIX + "shards.assigned";
		public static final String EVENT_PROCESSOR_POLLING_INTERVAL = EVENT_PROCESSOR_PREFIX + "polling_interval";
		public static final String EVENT_PROCESSOR_PULSE_INTERVAL = EVENT_PROCESSOR_PREFIX + "pulse_interval";
		public static final String EVENT_PROCESSOR_PULSE_EXPIRATION = EVENT_PROCESSOR_PREFIX + "pulse_expiration";
		public static final String EVENT_PROCESSOR_ORDER = EVENT_PROCESSOR_PREFIX + "order";
		public static final String EVENT_PROCESSOR_BATCH_SIZE = EVENT_PROCESSOR_PREFIX + "batch_size";
		public static final String EVENT_PROCESSOR_TRANSACTION_TIMEOUT = EVENT_PROCESSOR_PREFIX + "transaction_timeout";
		public static final String EVENT_PROCESSOR_RETRY_DELAY = EVENT_PROCESSOR_PREFIX + "retry_delay";
		public static final String MASS_INDEXER_PREFIX = "mass_indexer.";
		public static final String MASS_INDEXER_POLLING_INTERVAL = MASS_INDEXER_PREFIX + "polling_interval";
		public static final String MASS_INDEXER_PULSE_INTERVAL = MASS_INDEXER_PREFIX + "pulse_interval";
		public static final String MASS_INDEXER_PULSE_EXPIRATION = MASS_INDEXER_PREFIX + "pulse_expiration";

		public static final String ENTITY_MAPPING_PREFIX = "entity.mapping.";
		public static final String ENTITY_MAPPING_AGENT_PREFIX = ENTITY_MAPPING_PREFIX + "agent.";
		public static final String ENTITY_MAPPING_AGENT_TABLE = ENTITY_MAPPING_AGENT_PREFIX + "table";
		public static final String ENTITY_MAPPING_AGENT_SCHEMA = ENTITY_MAPPING_AGENT_PREFIX + "schema";
		public static final String ENTITY_MAPPING_AGENT_CATALOG = ENTITY_MAPPING_AGENT_PREFIX + "catalog";
		public static final String ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY = ENTITY_MAPPING_AGENT_PREFIX + "uuid_gen_strategy";
		public static final String ENTITY_MAPPING_AGENT_UUID_TYPE = ENTITY_MAPPING_AGENT_PREFIX + "uuid_type";
		public static final String ENTITY_MAPPING_OUTBOXEVENT_PREFIX = ENTITY_MAPPING_PREFIX + "outboxevent.";
		public static final String ENTITY_MAPPING_OUTBOXEVENT_TABLE = ENTITY_MAPPING_OUTBOXEVENT_PREFIX + "table";
		public static final String ENTITY_MAPPING_OUTBOXEVENT_SCHEMA = ENTITY_MAPPING_OUTBOXEVENT_PREFIX + "schema";
		public static final String ENTITY_MAPPING_OUTBOXEVENT_CATALOG = ENTITY_MAPPING_OUTBOXEVENT_PREFIX + "catalog";
		public static final String ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY =
				ENTITY_MAPPING_OUTBOXEVENT_PREFIX + "uuid_gen_strategy";
		public static final String ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE = ENTITY_MAPPING_OUTBOXEVENT_PREFIX + "uuid_type";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean COORDINATION_EVENT_PROCESSOR_ENABLED = true;
		public static final int COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL = 100;
		public static final int COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL = 2000;
		public static final int COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION = 30000;
		public static final OutboxEventProcessingOrder COORDINATION_EVENT_PROCESSOR_ORDER = OutboxEventProcessingOrder.AUTO;
		public static final int COORDINATION_EVENT_PROCESSOR_BATCH_SIZE = 50;
		public static final int COORDINATION_EVENT_PROCESSOR_RETRY_DELAY = 30;
		public static final int COORDINATION_MASS_INDEXER_POLLING_INTERVAL = 100;
		public static final int COORDINATION_MASS_INDEXER_PULSE_INTERVAL = 2000;
		public static final int COORDINATION_MASS_INDEXER_PULSE_EXPIRATION = 30000;

		// WARNING: Always use this prefix for all tables added by Hibernate Search:
		// we guarantee that in the documentation.
		public static final String HSEARCH_PREFIX = "HSEARCH_";

		// Must not be longer than 20 characters, so that the generator does not exceed the 30 characters for Oracle11g
		public static final String COORDINATION_ENTITY_MAPPING_AGENT_TABLE = HSEARCH_PREFIX + "AGENT";
		public static final UuidGenerationStrategy COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY =
				UuidGenerationStrategy.AUTO;
		public static final String COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE = "default";

		// Must not be longer than 20 characters, so that the generator does not exceed the 30 characters for Oracle11g
		public static final String COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE = HSEARCH_PREFIX + "OUTBOX_EVENT";
		public static final UuidGenerationStrategy COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_GEN_STRATEGY =
				UuidGenerationStrategy.AUTO;
		public static final String COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_TYPE = "default";
	}

	/**
	 * Builds a configuration property key for coordination, with the given radical.
	 * <p>
	 * See {@link CoordinationRadicals} for available radicals.
	 * <p>
	 * Example result: "{@code hibernate.search.coordination.tenant.myTenant.event_processor.enabled}"
	 *
	 * @param radical The radical of the configuration property (see constants in {@link CoordinationRadicals})
	 * @return the concatenated prefix + tenant ID + radical
	 */
	public static String coordinationKey(String radical) {
		return join( ".", HibernateOrmMapperSettings.COORDINATION, radical );
	}

	/**
	 * Builds a configuration property key for coordination for the given tenant, with the given radical.
	 * <p>
	 * See {@link CoordinationRadicals} for available radicals.
	 * <p>
	 * Example result: "{@code hibernate.search.coordination.tenant.myTenant.event_processor.enabled}"
	 *
	 * @param tenantId The identifier of the tenant whose coordination to configure, or {@code null} to configure defaults.
	 * @param radical The radical of the configuration property (see constants in {@link CoordinationRadicals})
	 * @return the concatenated prefix + tenant ID + radical
	 */
	public static String coordinationKey(String tenantId, String radical) {
		if ( tenantId == null ) {
			return coordinationKey( radical );
		}
		return join( ".", COORDINATION_TENANTS, tenantId, radical );
	}

}
