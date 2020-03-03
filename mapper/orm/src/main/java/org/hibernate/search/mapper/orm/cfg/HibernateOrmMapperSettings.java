/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
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
	 * Expects a {@link AutomaticIndexingStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_STRATEGY}.
	 *
	 * @see AutomaticIndexingStrategyName
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
		public static final String AUTOMATIC_INDEXING_STRATEGY = "automatic_indexing.strategy";
		public static final String AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY = "automatic_indexing.synchronization.strategy";
		public static final String AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = "automatic_indexing.enable_dirty_check";
		public static final String QUERY_LOADING_CACHE_LOOKUP_STRATEGY = "query.loading.cache_lookup.strategy";
		public static final String QUERY_LOADING_FETCH_SIZE = "query.loading.fetch_size";
		public static final String MAPPING_PROCESS_ANNOTATIONS = "mapping.process_annotations";
		public static final String MAPPING_CONFIGURER = "mapping.configurer";
		public static final String SCHEMA_MANAGEMENT_STRATEGY = "schema_management.strategy";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean ENABLED = true;
		public static final AutomaticIndexingStrategyName AUTOMATIC_INDEXING_STRATEGY =
				AutomaticIndexingStrategyName.SESSION;
		public static final BeanReference<AutomaticIndexingSynchronizationStrategy> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
				BeanReference.of( AutomaticIndexingSynchronizationStrategy.class, "write-sync" );
		public static final boolean AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = true;
		public static final EntityLoadingCacheLookupStrategy QUERY_LOADING_CACHE_LOOKUP_STRATEGY =
				EntityLoadingCacheLookupStrategy.SKIP;
		public static final int QUERY_LOADING_FETCH_SIZE = 100;
		public static final boolean MAPPING_PROCESS_ANNOTATIONS = true;
		public static final SchemaManagementStrategyName SCHEMA_MANAGEMENT_STRATEGY = SchemaManagementStrategyName.CREATE_OR_VALIDATE;
	}

}
