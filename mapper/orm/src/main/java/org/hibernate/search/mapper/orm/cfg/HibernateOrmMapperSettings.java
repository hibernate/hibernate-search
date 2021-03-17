/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyNames;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingStrategy;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class HibernateOrmMapperSettings {

	private HibernateOrmMapperSettings() {
	}

	/**
	 * The prefix expected for the key of every Hibernate Search configuration property
	 * when using the Hibernate ORM mapper.
	 */
	public static final String PREFIX = EngineSettings.PREFIX;

	/**
	 * Whether Hibernate Search is enabled or not.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#ENABLED}.
	 */
	public static final String ENABLED = PREFIX + Radicals.ENABLED;

	/**
	 * The automatic indexing strategy to use.
	 * <p>
	 * Expects one of the strings defined in {@link AutomaticIndexingStrategyNames},
	 * or a different string for a strategy provided by an external module.
	 * <p>
	 * For backward compatibility reasons, values of type {@link org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName}
	 * are also accepted, but are deprecated.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_STRATEGY}.
	 *
	 * @see AutomaticIndexingStrategyNames
	 */
	public static final String AUTOMATIC_INDEXING_STRATEGY = PREFIX + Radicals.AUTOMATIC_INDEXING_STRATEGY;

	/**
	 * The synchronization strategy to use when indexing automatically.
	 * <p>
	 * Expects one of the strings defined in {@link AutomaticIndexingSynchronizationStrategyNames},
	 * or a reference to a bean of type {@link AutomaticIndexingSynchronizationStrategy}.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY}.
	 *
	 * @see AutomaticIndexingSynchronizationStrategyNames
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY = PREFIX + Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY;

	/**
	 * Whether to check if dirty properties are relevant to indexing before actually reindexing an entity.
	 * <p>
	 * When enabled, re-indexing of an entity is skipped if the only changes are on properties that are not used when indexing.
	 * This feature is considered safe and thus enabled by default.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@code Defaults#AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK}.
	 */
	public static final String AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = PREFIX + Radicals.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK;

	/**
	 * Whether outbox events should be processed ({@code true})
	 * or left to accumulate without processing them ({@code false}).
	 * <p>
	 * Only available when {@link #AUTOMATIC_INDEXING_STRATEGY} is
	 * {@link AutomaticIndexingStrategyNames#OUTBOX_POLLING}.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE}.
	 */
	public static final String AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE = PREFIX + Radicals.AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE;

	/**
	 * The polling interval for the outbox events table, in milliseconds.
	 * <p>
	 * Only available when {@link #AUTOMATIC_INDEXING_STRATEGY} is
	 * {@link AutomaticIndexingStrategyNames#OUTBOX_POLLING} and
	 * {@link #AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE} is {@code true}.
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
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_POLLING_INTERVAL}.
	 */
	public static final String AUTOMATIC_INDEXING_POLLING_INTERVAL = PREFIX + Radicals.AUTOMATIC_INDEXING_POLLING_INTERVAL;

	/**
	 * How many outbox events to process in the same transaction.
	 * <p>
	 * Only available when {@link #AUTOMATIC_INDEXING_STRATEGY} is
	 * {@link AutomaticIndexingStrategyNames#OUTBOX_POLLING} and
	 * {@link #AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE} is {@code true}.
	 * <p>
	 * Expects a positive Integer value, such as {@code 50},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_BATCH_SIZE}.
	 */
	public static final String AUTOMATIC_INDEXING_BATCH_SIZE = PREFIX + Radicals.AUTOMATIC_INDEXING_BATCH_SIZE;

	/**
	 * The strategy to use when loading entities during the execution of a search query.
	 * <p>
	 * Expects a {@link EntityLoadingCacheLookupStrategy} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#QUERY_LOADING_CACHE_LOOKUP_STRATEGY}.
	 *
	 * @see EntityLoadingCacheLookupStrategy
	 */
	public static final String QUERY_LOADING_CACHE_LOOKUP_STRATEGY = PREFIX + Radicals.QUERY_LOADING_CACHE_LOOKUP_STRATEGY;

	/**
	 * The fetch size to use when loading entities during the execution of a search query.
	 * <p>
	 * Expects a strictly positive Integer value, such as {@code 100},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link Defaults#QUERY_LOADING_FETCH_SIZE}.
	 */
	public static final String QUERY_LOADING_FETCH_SIZE = PREFIX + Radicals.QUERY_LOADING_FETCH_SIZE;

	/**
	 * Whether annotations should be automatically processed for entity types,
	 * as well as nested types in those entity types, for instance embedded types.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@code Defaults#ENABLE_ANNOTATION_MAPPING}.
	 */
	public static final String MAPPING_PROCESS_ANNOTATIONS = PREFIX + Radicals.MAPPING_PROCESS_ANNOTATIONS;

	/**
	 * The mapping configurer to use.
	 * <p>
	 * Expects a reference to a bean of type {@link HibernateOrmSearchMappingConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String MAPPING_CONFIGURER = PREFIX + Radicals.MAPPING_CONFIGURER;

	/**
	 * The schema management strategy, controlling how indexes and their schema
	 * are created, updated, validated or dropped on startup and shutdown.
	 * <p>
	 * Expects a {@link SchemaManagementStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#SCHEMA_MANAGEMENT_STRATEGY}.
	 *
	 * @see SchemaManagementStrategyName
	 */
	public static final String SCHEMA_MANAGEMENT_STRATEGY = PREFIX + Radicals.SCHEMA_MANAGEMENT_STRATEGY;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static final class Radicals {

		private Radicals() {
		}

		public static final String ENABLED = "enabled";
		public static final String AUTOMATIC_INDEXING = "automatic_indexing";
		public static final String AUTOMATIC_INDEXING_PREFIX = AUTOMATIC_INDEXING + ".";
		public static final String AUTOMATIC_INDEXING_STRATEGY = AUTOMATIC_INDEXING_PREFIX + "strategy";
		public static final String AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY = AUTOMATIC_INDEXING_PREFIX + "synchronization.strategy";
		public static final String AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = AUTOMATIC_INDEXING_PREFIX + "enable_dirty_check";
		public static final String AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE = AUTOMATIC_INDEXING_PREFIX + AutomaticIndexingRadicals.PROCESS_OUTBOX_TABLE;
		public static final String AUTOMATIC_INDEXING_POLLING_INTERVAL = AUTOMATIC_INDEXING_PREFIX + AutomaticIndexingRadicals.POLLING_INTERVAL;
		public static final String AUTOMATIC_INDEXING_BATCH_SIZE = AUTOMATIC_INDEXING_PREFIX + AutomaticIndexingRadicals.BATCH_SIZE;
		public static final String QUERY_LOADING_CACHE_LOOKUP_STRATEGY = "query.loading.cache_lookup.strategy";
		public static final String QUERY_LOADING_FETCH_SIZE = "query.loading.fetch_size";
		public static final String MAPPING_PROCESS_ANNOTATIONS = "mapping.process_annotations";
		public static final String MAPPING_CONFIGURER = "mapping.configurer";
		public static final String SCHEMA_MANAGEMENT_STRATEGY = "schema_management.strategy";
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix} + {@link Radicals#AUTOMATIC_INDEXING_PREFIX}.
	 */
	public static final class AutomaticIndexingRadicals {

		private AutomaticIndexingRadicals() {
		}

		public static final String PROCESS_OUTBOX_TABLE = "process_outbox_table";
		public static final String POLLING_INTERVAL = "polling_interval";
		public static final String BATCH_SIZE = "batch_size";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean ENABLED = true;
		public static final BeanReference<AutomaticIndexingStrategy> AUTOMATIC_INDEXING_STRATEGY =
				BeanReference.of( AutomaticIndexingStrategy.class, AutomaticIndexingStrategyNames.SESSION );
		public static final BeanReference<AutomaticIndexingSynchronizationStrategy> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
				BeanReference.of( AutomaticIndexingSynchronizationStrategy.class, "write-sync" );
		public static final boolean AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = true;
		public static final boolean AUTOMATIC_INDEXING_PROCESS_OUTBOX_TABLE = true;
		public static final int AUTOMATIC_INDEXING_POLLING_INTERVAL = 8;
		public static final int AUTOMATIC_INDEXING_BATCH_SIZE = 50;
		public static final EntityLoadingCacheLookupStrategy QUERY_LOADING_CACHE_LOOKUP_STRATEGY =
				EntityLoadingCacheLookupStrategy.SKIP;
		public static final int QUERY_LOADING_FETCH_SIZE = 100;
		public static final boolean MAPPING_PROCESS_ANNOTATIONS = true;
		public static final SchemaManagementStrategyName SCHEMA_MANAGEMENT_STRATEGY = SchemaManagementStrategyName.CREATE_OR_VALIDATE;
	}

}
