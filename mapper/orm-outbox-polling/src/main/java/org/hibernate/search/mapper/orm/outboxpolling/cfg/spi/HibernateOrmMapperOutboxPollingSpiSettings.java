/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.cfg.spi;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cfg.HibernateOrmMapperOutboxPollingSettings;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.OutboxPollingAgentAdditionalJaxbMappingProducer;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxPollingOutboxEventAdditionalJaxbMappingProducer;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * SPI-related settings.
 */
@Incubating
public final class HibernateOrmMapperOutboxPollingSpiSettings {

	private HibernateOrmMapperOutboxPollingSpiSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property
	 * when using the Hibernate ORM mapper.
	 */
	public static final String PREFIX = HibernateOrmMapperSettings.PREFIX;

	/**
	 * Allows the user to define a specific Hibernate mapping for the outbox event table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects a String value containing the xml expressing the Hibernate mapping for the entity.
	 * <p>
	 * The default for this value is {@link OutboxPollingOutboxEventAdditionalJaxbMappingProducer#ENTITY_DEFINITION}
	 * <p>
	 * As this configuration entirely overrides the entity mapping it cannot be used in combination with any properties
	 * that define names of catalog/schema/table/identity generator for the outbox event table (
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_CATALOG},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_SCHEMA},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_TABLE},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_GEN_STRATEGY},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_OUTBOXEVENT_UUID_TYPE}.
	 * An exception ({@link org.hibernate.search.util.common.SearchException} will be thrown in case of such misconfiguration.
	 */
	public static final String OUTBOXEVENT_ENTITY_MAPPING = PREFIX + Radicals.OUTBOXEVENT_ENTITY_MAPPING;

	/**
	 * Allows the user to define a specific Hibernate mapping for the agent table.
	 * <p>
	 * Only available when {@value HibernateOrmMapperSettings#COORDINATION_STRATEGY} is
	 * {@value HibernateOrmMapperOutboxPollingSettings#COORDINATION_STRATEGY_NAME}.
	 * <p>
	 * Expects a String value containing the xml expressing the Hibernate mapping for the entity.
	 * <p>
	 * The default for this value is {@link OutboxPollingAgentAdditionalJaxbMappingProducer#ENTITY_DEFINITION}
	 * <p>
	 * As this configuration entirely overrides the entity mapping it cannot be used in combination with any properties
	 * that define names of catalog/schema/table/identity generator for the agent table (
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_AGENT_CATALOG},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_AGENT_SCHEMA},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_AGENT_TABLE},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY},
	 * {@link HibernateOrmMapperOutboxPollingSettings#COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE}.
	 * An exception ({@link org.hibernate.search.util.common.SearchException} will be thrown in case of such misconfiguration.
	 */
	public static final String AGENT_ENTITY_MAPPING = PREFIX + Radicals.AGENT_ENTITY_MAPPING;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static final class Radicals {

		private Radicals() {
		}

		public static final String COORDINATION_PREFIX = HibernateOrmMapperSettings.Radicals.COORDINATION_PREFIX;

		public static final String OUTBOXEVENT_ENTITY_MAPPING =
				COORDINATION_PREFIX + CoordinationRadicals.OUTBOXEVENT_ENTITY_MAPPING;
		public static final String AGENT_ENTITY_MAPPING = COORDINATION_PREFIX + CoordinationRadicals.AGENT_ENTITY_MAPPING;

	}

	public static final class CoordinationRadicals {

		private CoordinationRadicals() {
		}

		public static final String OUTBOXEVENT_ENTITY_MAPPING = "outboxevent.entity.mapping";
		public static final String AGENT_ENTITY_MAPPING = "agent.entity.mapping";

	}

}
