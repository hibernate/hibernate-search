/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.cfg;

import org.hibernate.search.engine.environment.bean.BeanReference;
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

	public static class Radicals {

		private Radicals() {
		}

		public static final String SCHEMA_MANAGEMENT_STRATEGY = "schema_management.strategy";
		public static final String MAPPING_CONFIGURER = "mapping.configurer";
		public static final String MULTI_TENANCY_ENABLED = "mapping.multi_tenancy.enabled";
		public static final String MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER =
				"mapping.multi_tenancy.tenant_identifier_converter";
		public static final String INDEXING_PLAN_SYNCHRONIZATION_PREFIX = "indexing.plan.synchronization.";
		public static final String INDEXING_PLAN_SYNCHRONIZATION_STRATEGY = INDEXING_PLAN_SYNCHRONIZATION_PREFIX + "strategy";

	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final SchemaManagementStrategyName SCHEMA_MANAGEMENT_STRATEGY =
				SchemaManagementStrategyName.CREATE_OR_VALIDATE;
		public static final boolean MULTI_TENANCY_ENABLED = false;

		public static final BeanReference<IndexingPlanSynchronizationStrategy> INDEXING_PLAN_SYNCHRONIZATION_STRATEGY =
				BeanReference.of( IndexingPlanSynchronizationStrategy.class, "write-sync" );

		public static final BeanReference<TenantIdentifierConverter> MULTI_TENANCY_TENANT_IDENTIFIER_CONVERTER =
				BeanReference.of( TenantIdentifierConverter.class, StringTenantIdentifierConverter.NAME );
	}

}
