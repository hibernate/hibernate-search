/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.cfg;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.tenancy.TenantIdentifierConverter;
import org.hibernate.search.mapper.pojo.tenancy.spi.StringTenantIdentifierConverter;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class StandalonePojoMapperSettings {

	private StandalonePojoMapperSettings() {
	}

	public static final String PREFIX = "hibernate.search.";

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
	 * Whether Hibernate Search should automatically build Jandex indexes for types registered for annotation processing
	 * (entities in particular),
	 * to ensure that all "root mapping" annotations in those JARs (e.g. {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor})
	 * are taken into account.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed into a Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES}.
	 *
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext#buildMissingDiscoveredJandexIndexes(boolean)
	 */
	public static final String MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES =
			PREFIX + Radicals.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES;

	/**
	 * Whether Hibernate Search should automatically discover annotated types
	 * present in the Jandex index that are also annotated
	 * with {@link RootMapping root mapping annotations}.
	 * <p>
	 * When enabled, if an annotation meta-annotated with {@link RootMapping}
	 * is found in the Jandex index,
	 * and a type annotated with that annotation (e.g. {@link SearchEntity} or {@link ProjectionConstructor}) is found in the Jandex index,
	 * then that type will automatically be scanned for mapping annotations,
	 * even if the type wasn't explicitly added.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed into a Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS}.
	 *
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext#discoverAnnotatedTypesFromRootMappingAnnotations(boolean)
	 */
	public static final String MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS =
			PREFIX + Radicals.MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS;

	/**
	 * A configurer for the Hibernate Search mapping.
	 * <p>
	 * Expects a single-valued or multi-valued reference to beans of type {@link StandalonePojoMappingConfigurer}.
	 * <p>
	 * Defaults to no value.
	 *
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "multi-valued bean reference" properties and accepted values.
	 */
	public static final String MAPPING_CONFIGURER = PREFIX + Radicals.MAPPING_CONFIGURER;

	/**
	 * Enables or disables multi-tenancy.
	 * <p>
	 * If multi-tenancy is enabled, every {@link SearchMapping#createSession() session} will need to be assigned a tenant identifier.
	 * <p>
	 * Expects a boolean value.
	 * <p>
	 * Defaults to {@link Defaults#MULTI_TENANCY_ENABLED}.
	 */
	public static final String MULTI_TENANCY_ENABLED = PREFIX + Radicals.MULTI_TENANCY_ENABLED;

	/**
	 * How to convert tenant identifier to and form the string representation.
	 * <p>
	 * When multi-tenancy is enabled, and non-string tenant identifiers are used
	 * a custom converter <strong>must</strong> be provided through this property.
	 * <p>
	 * Defaults to {@link Defaults#MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER}.
	 * This converter only supports string tenant identifiers and will fail if some other type of identifiers is used.
	 * @see TenantIdentifierConverter
	 */
	public static final String MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER =
			PREFIX + Radicals.MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER;

	/**
	 * How to synchronize between application threads and indexing triggered by the
	 * {@link org.hibernate.search.mapper.pojo.standalone.session.SearchSession SearchSession}'s
	 * {@link org.hibernate.search.mapper.pojo.standalone.session.SearchSession#indexingPlan indexing plan}.
	 * <p>
	 * Expects one of the strings defined in {@link IndexingPlanSynchronizationStrategy},
	 * or a reference to a bean of type {@link IndexingPlanSynchronizationStrategy}.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_PLAN_SYNCHRONIZATION_STRATEGY}.
	 *
	 * @see IndexingPlanSynchronizationStrategyNames
	 * @see org.hibernate.search.engine.cfg The core documentation of configuration properties,
	 * which includes a description of the "bean reference" properties and accepted values.
	 */
	public static final String INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
			PREFIX + Radicals.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY;

	/**
	 * The default index cleaning operation to apply during mass indexing,
	 * unless configured explicitly.
	 * <p>
	 * Expects a {@link MassIndexingDefaultCleanOperation} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_MASS_DEFAULT_CLEAN_OPERATION}.
	 */
	public static final String INDEXING_MASS_DEFAULT_CLEAN_OPERATION = PREFIX + Radicals.INDEXING_MASS_DEFAULT_CLEAN_OPERATION;

	public static class Radicals {

		private Radicals() {
		}

		public static final String SCHEMA_MANAGEMENT_STRATEGY = "schema_management.strategy";
		public static final String MAPPING_PREFIX = "mapping.";
		public static final String MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES =
				MAPPING_PREFIX + "build_missing_discovered_jandex_indexes";
		public static final String MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS =
				MAPPING_PREFIX + "discover_annotated_types_from_root_mapping_annotations";
		public static final String MAPPING_CONFIGURER = MAPPING_PREFIX + "configurer";
		public static final String MULTI_TENANCY_ENABLED = MAPPING_PREFIX + "multi_tenancy.enabled";
		public static final String MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER =
				MAPPING_PREFIX + "multi_tenancy.tenant_identifier_converter";
		public static final String INDEXING_PLAN_SYNCHRONIZATION_PREFIX = "indexing.plan.synchronization.";
		public static final String INDEXING_PLAN_SYNCHRONIZATION_STRATEGY = INDEXING_PLAN_SYNCHRONIZATION_PREFIX + "strategy";
		public static final String INDEXING_MASS_DEFAULT_CLEAN_OPERATION = "indexing.mass.default_clean_operation";

	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final SchemaManagementStrategyName SCHEMA_MANAGEMENT_STRATEGY =
				SchemaManagementStrategyName.CREATE_OR_VALIDATE;
		public static final boolean MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES = true;
		public static final boolean MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS = true;
		public static final boolean MULTI_TENANCY_ENABLED = false;

		public static final BeanReference<IndexingPlanSynchronizationStrategy> INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
				BeanReference.of( IndexingPlanSynchronizationStrategy.class, "write-sync" );

		public static final BeanReference<TenantIdentifierConverter> MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER =
				BeanReference.of( TenantIdentifierConverter.class, StringTenantIdentifierConverter.NAME );
		public static final MassIndexingDefaultCleanOperation INDEXING_MASS_DEFAULT_CLEAN_OPERATION =
				MassIndexingDefaultCleanOperation.PURGE;
	}

}
