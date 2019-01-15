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

	public static final String PREFIX = "hibernate.search.";

	/**
	 * When enabled, Hibernate Search will track the parts of the provided configuration that are actually used
	 * and log a warning if any configuration property is never used, which might indicate a configuration issue.
	 * <p>
	 * Enabled by default.
	 */
	public static final String ENABLE_CONFIGURATION_PROPERTY_TRACKING =
			PREFIX + Radicals.ENABLE_CONFIGURATION_PROPERTY_TRACKING;

	/**
	 * Enable listeners auto registration in Hibernate Annotations and EntityManager. Default to true.
	 */
	public static final String AUTOREGISTER_LISTENERS = PREFIX + Radicals.AUTOREGISTER_LISTENERS;

	/**
	 * Defines the indexing strategy, default <code>event</code>
	 * Other options <code>manual</code>
	 */
	public static final String INDEXING_STRATEGY = PREFIX + Radicals.INDEXING_STRATEGY;

	/**
	 * When enabled re-indexing of an entity is skipped if the updates affect only non-indexed fields.
	 * Enabled by default as it should be safe and should improve performance, disable it to force updates
	 * skipping value checks.
	 * Affect semantics of entity updates only.
	 */
	public static final String ENABLE_DIRTY_CHECK = PREFIX + Radicals.ENABLE_DIRTY_CHECK;

	/**
	 * When enabled, annotations will be automatically processed for entity types,
	 * as well as nested types in those entity types, for instance embedded types.
	 * Enabled by default. Disable to only consider {@link #MAPPING_CONFIGURER}.
	 */
	public static final String ENABLE_ANNOTATION_MAPPING = PREFIX + Radicals.ENABLE_ANNOTATION_MAPPING;

	/**
	 * Configure a programmatic mapping in Hibernate Search.
	 * <p>
	 * Accepts a {@link HibernateOrmSearchMappingConfigurer}
	 * instance or the fully qualified class name of a {@link HibernateOrmSearchMappingConfigurer} subclass.
	 * Such a subclass must have a no-arg constructor.
	 */
	public static final String MAPPING_CONFIGURER = PREFIX + Radicals.MAPPING_CONFIGURER;

	public static class Radicals {
		public static final String ENABLE_CONFIGURATION_PROPERTY_TRACKING = "enable_configuration_property_tracking";
		public static final String AUTOREGISTER_LISTENERS = "autoregister_listeners";
		public static final String INDEXING_STRATEGY = "indexing_strategy";
		public static final String ENABLE_DIRTY_CHECK = "enable_dirty_check";
		public static final String ENABLE_ANNOTATION_MAPPING = "enable_annotation_mapping";
		public static final String MAPPING_CONFIGURER = "mapping_configurer";

		private Radicals() {
		}
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean ENABLE_CONFIGURATION_PROPERTY_TRACKING = true;
		public static final boolean AUTOREGISTER_LISTENERS = true;
		public static final IndexingStrategyConfiguration INDEXING_STRATEGY = IndexingStrategyConfiguration.EVENT;
		public static final boolean ENABLE_DIRTY_CHECK = true;
		public static final boolean ENABLE_ANNOTATION_MAPPING = true;
	}

	private HibernateOrmMapperSettings() {
	}

}
