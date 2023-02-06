/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.cfg;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategy;
import org.hibernate.search.mapper.orm.coordination.impl.NoCoordinationStrategy;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@HibernateSearchConfiguration(
		title = "Hibernate Search ORM Integration",
		anchorPrefix = "hibernate-search-mapper-orm-"
)
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
	 * Whether automatic indexing is enabled, i.e. whether changes to entities in a Hibernate ORM session
	 * are detected automatically and lead to reindexing.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_ENABLED}.
	 */
	public static final String AUTOMATIC_INDEXING_ENABLED = PREFIX + Radicals.AUTOMATIC_INDEXING_ENABLED;

	/**
	 * The automatic indexing strategy to use.
	 * <p>
	 * Expects a {@link org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_STRATEGY}.
	 *
	 * @deprecated Use {@link #AUTOMATIC_INDEXING_ENABLED} instead (caution: it expects a boolean value).
	 */
	@Deprecated
	public static final String AUTOMATIC_INDEXING_STRATEGY = PREFIX + Radicals.AUTOMATIC_INDEXING_STRATEGY;

	/**
	 * The synchronization strategy to use when indexing automatically or explicitly
	 * through a {@link org.hibernate.search.mapper.orm.work.SearchIndexingPlan SearchIndexingPlan}.
	 * <p>
	 * Expects one of the strings defined in {@link org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames},
	 * or a reference to a bean of type {@link org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy}.
	 * <p>
	 * Defaults to {@link Defaults#AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY}.
	 *
	 * @see org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 *
	 * @deprecated Use {@link #INDEXING_PLAN_SYNCHRONIZATION_STRATEGY} instead.
	 */
	@Deprecated
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
	 * Defaults to {@link Defaults#MAPPING_PROCESS_ANNOTATIONS}.
	 */
	public static final String MAPPING_PROCESS_ANNOTATIONS = PREFIX + Radicals.MAPPING_PROCESS_ANNOTATIONS;

	/**
	 * When annotation processing is enabled,
	 * whether Hibernate Search should automatically build Jandex indexes for types registered for annotation processing
	 * (entities in particular),
	 * to ensure that all "root mapping" annotations in those JARs (e.g. {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor})
	 * are taken into account..
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES}.
	 */
	public static final String MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES = PREFIX + Radicals.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES;

	/**
	 * The mapping configurer to use.
	 * <p>
	 * Expects a single-valued or multi-valued reference to beans of type {@link HibernateOrmSearchMappingConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "multi-valued bean reference" properties and accepted values.
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
	 * The root property for properties related to coordination.
	 */
	public static final String COORDINATION = PREFIX + HibernateOrmMapperSettings.Radicals.COORDINATION;

	/**
	 * The strategy for coordinating between nodes of a distributed application.
	 * <p>
	 * Expects a reference to a coordination strategy;
	 * see the reference documentation for available strategies
	 * and the relevant Maven dependencies.
	 * <p>
	 * Defaults to {@link Defaults#COORDINATION_STRATEGY}.
	 */
	public static final String COORDINATION_STRATEGY = PREFIX + Radicals.COORDINATION_STRATEGY;

	/**
	 * An exhaustive list of all tenant identifiers that can be used by the application when multi-tenancy is enabled.
	 * <p>
	 * Expects either a String representing multiple tenant IDs separated by commas,
	 * or a {@code Collection<String>} containing tenant IDs.
	 * <p>
	 * No default; this property may have to be set explicitly depending on the
	 * {@link #COORDINATION_STRATEGY coordination strategy}.
	 */
	public static final String MULTI_TENANCY_TENANT_IDS = PREFIX + Radicals.MULTI_TENANCY_TENANT_IDS;

	/**
	 * The synchronization strategy to use when indexing automatically or explicitly
	 * through a {@link org.hibernate.search.mapper.orm.work.SearchIndexingPlan SearchIndexingPlan}.
	 * <p>
	 * Expects one of the strings defined in {@link IndexingPlanSynchronizationStrategyNames},
	 * or a reference to a bean of type {@link IndexingPlanSynchronizationStrategy}.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_PLAN_SYNCHRONIZATION_STRATEGY}.
	 *
	 * @see IndexingPlanSynchronizationStrategyNames
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String INDEXING_PLAN_SYNCHRONIZATION_STRATEGY = PREFIX + Radicals.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY;

	/**
	 * Configuration property keys without the {@link #PREFIX prefix}.
	 */
	public static final class Radicals {

		private Radicals() {
		}

		public static final String ENABLED = "enabled";
		public static final String AUTOMATIC_INDEXING = "automatic_indexing";
		public static final String AUTOMATIC_INDEXING_PREFIX = AUTOMATIC_INDEXING + ".";
		public static final String AUTOMATIC_INDEXING_ENABLED = AUTOMATIC_INDEXING_PREFIX + AutomaticIndexingRadicals.ENABLED;
		/**
		 * @deprecated Use {@link #AUTOMATIC_INDEXING_ENABLED} instead (caution: it expects a boolean value).
		 */
		@Deprecated
		public static final String AUTOMATIC_INDEXING_STRATEGY = AUTOMATIC_INDEXING_PREFIX + AutomaticIndexingRadicals.STRATEGY;
		/**
		 * @deprecated Use {@link  #INDEXING_PLAN_SYNCHRONIZATION_STRATEGY} instead.
		 */
		@Deprecated
		public static final String AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY = AUTOMATIC_INDEXING_PREFIX + AutomaticIndexingRadicals.SYNCHRONIZATION_STRATEGY;
		public static final String AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = AUTOMATIC_INDEXING_PREFIX + AutomaticIndexingRadicals.ENABLE_DIRTY_CHECK;
		public static final String QUERY_LOADING_CACHE_LOOKUP_STRATEGY = "query.loading.cache_lookup.strategy";
		public static final String QUERY_LOADING_FETCH_SIZE = "query.loading.fetch_size";
		public static final String MAPPING_PROCESS_ANNOTATIONS = "mapping.process_annotations";
		public static final String MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES = "mapping.build_missing_discovered_jandex_indexes";
		public static final String MAPPING_CONFIGURER = "mapping.configurer";
		public static final String SCHEMA_MANAGEMENT_STRATEGY = "schema_management.strategy";
		public static final String COORDINATION = "coordination";
		public static final String COORDINATION_PREFIX = COORDINATION + ".";
		public static final String COORDINATION_STRATEGY = COORDINATION_PREFIX + CoordinationRadicals.STRATEGY;
		public static final String MULTI_TENANCY = "multi_tenancy";
		public static final String MULTI_TENANCY_PREFIX = MULTI_TENANCY + ".";
		public static final String MULTI_TENANCY_TENANT_IDS = MULTI_TENANCY_PREFIX + MultiTenancyRadicals.TENANT_IDS;
		public static final String INDEXING_PLAN_PREFIX = "indexing.plan.";
		public static final String INDEXING_PLAN_SYNCHRONIZATION_STRATEGY = INDEXING_PLAN_PREFIX + IndexingPlanRadicals.SYNCHRONIZATION_STRATEGY;

	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix} + {@link Radicals#AUTOMATIC_INDEXING_PREFIX}.
	 */
	public static final class AutomaticIndexingRadicals {

		private AutomaticIndexingRadicals() {
		}

		public static final String ENABLED = "enabled";
		/**
		 * @deprecated Use {@link #ENABLED} instead (caution: it expects a boolean value).
		 */
		@Deprecated
		public static final String STRATEGY = "strategy";
		/**
		 * @deprecated Use {@link IndexingPlanRadicals#SYNCHRONIZATION_STRATEGY} instead.
		 */
		@Deprecated
		public static final String SYNCHRONIZATION_STRATEGY = "synchronization.strategy";
		public static final String ENABLE_DIRTY_CHECK = "enable_dirty_check";
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix} + {@link Radicals#INDEXING_PLAN_PREFIX}.
	 */
	public static final class IndexingPlanRadicals {

		private IndexingPlanRadicals() {
		}

		public static final String SYNCHRONIZATION_STRATEGY = "synchronization.strategy";
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix} + {@link Radicals#COORDINATION_PREFIX}.
	 */
	public static final class CoordinationRadicals {

		private CoordinationRadicals() {
		}

		public static final String STRATEGY = "strategy";
	}

	/**
	 * Configuration property keys without the {@link #PREFIX prefix} + {@link Radicals#MULTI_TENANCY_PREFIX}.
	 */
	public static final class MultiTenancyRadicals {

		private MultiTenancyRadicals() {
		}

		public static final String TENANT_IDS = "tenant_ids";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean ENABLED = true;
		public static final boolean AUTOMATIC_INDEXING_ENABLED = true;
		/**
		 * @deprecated Use the new configuration property instead:
		 * {@link HibernateOrmMapperSettings#AUTOMATIC_INDEXING_STRATEGY},
		 * (caution: it expects a boolean value, and its default is {@link #ENABLED}).
		 */
		@Deprecated
		public static final org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName AUTOMATIC_INDEXING_STRATEGY =
				org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName.SESSION;
		/**
		 * @deprecated Use {@link #INDEXING_PLAN_SYNCHRONIZATION_STRATEGY} instead.
		 */
		@Deprecated
		public static final BeanReference<org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy> AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY =
				BeanReference.of( org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy.class, "write-sync" );
		public static final boolean AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK = true;
		public static final EntityLoadingCacheLookupStrategy QUERY_LOADING_CACHE_LOOKUP_STRATEGY =
				EntityLoadingCacheLookupStrategy.SKIP;
		public static final int QUERY_LOADING_FETCH_SIZE = 100;
		public static final boolean MAPPING_PROCESS_ANNOTATIONS = true;
		public static final boolean MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES = true;
		public static final SchemaManagementStrategyName SCHEMA_MANAGEMENT_STRATEGY = SchemaManagementStrategyName.CREATE_OR_VALIDATE;
		public static final BeanReference<CoordinationStrategy> COORDINATION_STRATEGY =
				BeanReference.of( CoordinationStrategy.class, NoCoordinationStrategy.NAME );
		public static final BeanReference<IndexingPlanSynchronizationStrategy> INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
				BeanReference.of( IndexingPlanSynchronizationStrategy.class, "write-sync" );

	}

}
