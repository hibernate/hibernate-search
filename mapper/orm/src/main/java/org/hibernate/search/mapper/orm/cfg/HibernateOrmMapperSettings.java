/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class HibernateOrmMapperSettings {

	private HibernateOrmMapperSettings() {
	}

	public static final String PREFIX = "hibernate.search.";

	/**
	 * Whether usage of configuration property should be tracked.
	 * <p>
	 * When enabled, Hibernate Search will track the parts of the provided configuration that are actually used
	 * and log a warning if any configuration property is never used, which might indicate a configuration issue.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#ENABLE_CONFIGURATION_PROPERTY_TRACKING}.
	 */
	public static final String ENABLE_CONFIGURATION_PROPERTY_TRACKING =
			PREFIX + Radicals.ENABLE_CONFIGURATION_PROPERTY_TRACKING;

	/**
	 * Whether Hibernate Search should automatically register listeners to entity changes,
	 * so that changes to entities result in automatic indexing.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOREGISTER_LISTENERS}.
	 */
	public static final String AUTOREGISTER_LISTENERS = PREFIX + Radicals.AUTOREGISTER_LISTENERS;

	/**
	 * The indexing strategy to use.
	 * <p>
	 * Expects a {@link HibernateOrmIndexingStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_STRATEGY}.
	 */
	public static final String INDEXING_STRATEGY = PREFIX + Radicals.INDEXING_STRATEGY;

	/**
	 * Whether to check if dirty properties are relevant to indexing before actually reindexing an entity.
	 * <p>
	 * When enabled, re-indexing of an entity is skipped if the only changes are on properties that are not used when indexing.
	 * This feature is considered safe and thus enabled by default.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@code Defaults#ENABLE_DIRTY_CHECK}.
	 */
	public static final String ENABLE_DIRTY_CHECK = PREFIX + Radicals.ENABLE_DIRTY_CHECK;

	/**
	 * Whether annotations should be automatically processed for entity types,
	 * as well as nested types in those entity types, for instance embedded types.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@code Defaults#ENABLE_ANNOTATION_MAPPING}.
	 */
	public static final String ENABLE_ANNOTATION_MAPPING = PREFIX + Radicals.ENABLE_ANNOTATION_MAPPING;

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

	public static class Radicals {

		private Radicals() {
		}

		public static final String ENABLE_CONFIGURATION_PROPERTY_TRACKING = "enable_configuration_property_tracking";
		public static final String AUTOREGISTER_LISTENERS = "autoregister_listeners";
		public static final String INDEXING_STRATEGY = "indexing_strategy";
		public static final String ENABLE_DIRTY_CHECK = "enable_dirty_check";
		public static final String ENABLE_ANNOTATION_MAPPING = "enable_annotation_mapping";
		public static final String MAPPING_CONFIGURER = "mapping_configurer";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean ENABLE_CONFIGURATION_PROPERTY_TRACKING = true;
		public static final boolean AUTOREGISTER_LISTENERS = true;
		public static final HibernateOrmIndexingStrategyName INDEXING_STRATEGY = HibernateOrmIndexingStrategyName.EVENT;
		public static final boolean ENABLE_DIRTY_CHECK = true;
		public static final boolean ENABLE_ANNOTATION_MAPPING = true;
	}

}
