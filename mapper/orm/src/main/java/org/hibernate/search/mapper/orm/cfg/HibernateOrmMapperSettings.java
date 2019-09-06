/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

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
	public static final String PREFIX = "hibernate.search.";

	/**
	 * The strategy to use when reporting the results of configuration property checking.
	 * <p>
	 * Configuration property checking will detect an configuration property that is never used,
	 * which might indicate a configuration issue.
	 * <p>
	 * Expects a {@link HibernateOrmConfigurationPropertyCheckingStrategyName} value,
	 * or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#CONFIGURATION_PROPERTY_CHECKING_STRATEGY}.
	 */
	public static final String CONFIGURATION_PROPERTY_CHECKING_STRATEGY =
			PREFIX + Radicals.CONFIGURATION_PROPERTY_CHECKING_STRATEGY;

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
	 * Expects a {@link HibernateOrmAutomaticIndexingStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_STRATEGY}.
	 *
	 * @see HibernateOrmAutomaticIndexingStrategyName
	 */
	public static final String AUTOMATIC_INDEXING_STRATEGY = PREFIX + Radicals.AUTOMATIC_INDEXING_STRATEGY;

	/**
	 * The synchronization strategy to use when indexing automatically.
	 * <p>
	 * Expects a {@link HibernateOrmAutomaticIndexingSynchronizationStrategyName} value,
	 * or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY}.
	 *
	 * @see HibernateOrmAutomaticIndexingSynchronizationStrategyName
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
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static class Radicals {

		private Radicals() {
		}

		public static final String ENABLED = "enabled";
		public static final String CONFIGURATION_PROPERTY_CHECKING_STRATEGY = "configuration_property_checking.strategy";
		public static final String AUTOMATIC_INDEXING_STRATEGY = "automatic_indexing.strategy";
		public static final String AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY = "automatic_indexing.synchronization.strategy";
		public static final String AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = "automatic_indexing.enable_dirty_check";
		public static final String QUERY_LOADING_CACHE_LOOKUP_STRATEGY = "query.loading.cache_lookup.strategy";
		public static final String QUERY_LOADING_FETCH_SIZE = "query.loading.fetch_size";
		public static final String MAPPING_PROCESS_ANNOTATIONS = "mapping.process_annotations";
		public static final String MAPPING_CONFIGURER = "mapping.configurer";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final HibernateOrmConfigurationPropertyCheckingStrategyName CONFIGURATION_PROPERTY_CHECKING_STRATEGY =
				HibernateOrmConfigurationPropertyCheckingStrategyName.WARN;
		public static final boolean ENABLED = true;
		public static final HibernateOrmAutomaticIndexingStrategyName AUTOMATIC_INDEXING_STRATEGY =
				HibernateOrmAutomaticIndexingStrategyName.SESSION;
		public static final HibernateOrmAutomaticIndexingSynchronizationStrategyName AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
				HibernateOrmAutomaticIndexingSynchronizationStrategyName.COMMITTED;
		public static final boolean AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = true;
		public static final EntityLoadingCacheLookupStrategy QUERY_LOADING_CACHE_LOOKUP_STRATEGY =
				EntityLoadingCacheLookupStrategy.SKIP;
		public static final int QUERY_LOADING_FETCH_SIZE = 100;
		public static final boolean MAPPING_PROCESS_ANNOTATIONS = true;
	}

}
