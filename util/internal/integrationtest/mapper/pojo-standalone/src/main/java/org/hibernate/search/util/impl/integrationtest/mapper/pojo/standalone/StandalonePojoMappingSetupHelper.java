/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMappingBuilder;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.bean.ForbiddenBeanProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

public final class StandalonePojoMappingSetupHelper
		extends
		MappingSetupHelper<StandalonePojoMappingSetupHelper.SetupContext,
				SearchMappingBuilder,
				StandalonePojoMappingConfigurationContext,
				CloseableSearchMapping,
				StandalonePojoMappingSetupHelper.SetupVariant> {

	/**
	 * @param lookup A {@link MethodHandles.Lookup} with private access to the test method,
	 * to be passed to mapping builders created by {@link SetupContext#setup(Class[])} or {@link SetupContext#setup()}
	 * so that the Standalone POJO mapper will be able to inspect classes defined in the test methods.
	 * @param backendMock A backend mock.
	 */
	public static StandalonePojoMappingSetupHelper withBackendMock(MethodHandles.Lookup lookup, BackendMock backendMock) {
		return new StandalonePojoMappingSetupHelper( lookup, BackendSetupStrategy.withSingleBackendMock( backendMock ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE );
	}

	public static StandalonePojoMappingSetupHelper withBackendMocks(MethodHandles.Lookup lookup,
			BackendMock defaultBackendMock, Map<String, BackendMock> namedBackendMocks) {
		return new StandalonePojoMappingSetupHelper(
				lookup,
				BackendSetupStrategy.withMultipleBackendMocks( defaultBackendMock, namedBackendMocks ),
				// Mock backend => avoid schema management unless we want to test it
				SchemaManagementStrategyName.NONE
		);
	}

	public static StandalonePojoMappingSetupHelper withSingleBackend(MethodHandles.Lookup lookup,
			BackendConfiguration backendConfiguration) {
		return new StandalonePojoMappingSetupHelper(
				lookup,
				BackendSetupStrategy.withSingleBackend( backendConfiguration ),
				// Real backend => ensure we clean up everything before and after the tests
				SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP
		);
	}

	private final MethodHandles.Lookup lookup;
	private final SchemaManagementStrategyName schemaManagementStrategyName;
	private final StandalonePojoAssertionHelper assertionHelper;

	private ReindexOnUpdate defaultReindexOnUpdate;

	private StandalonePojoMappingSetupHelper(MethodHandles.Lookup lookup, BackendSetupStrategy backendSetupStrategy,
			SchemaManagementStrategyName schemaManagementStrategyName) {
		super( backendSetupStrategy );
		this.lookup = lookup;
		this.schemaManagementStrategyName = schemaManagementStrategyName;
		this.assertionHelper = new StandalonePojoAssertionHelper( backendSetupStrategy );
	}

	public StandalonePojoMappingSetupHelper disableAssociationReindexing() {
		this.defaultReindexOnUpdate = ReindexOnUpdate.SHALLOW;
		return this;
	}

	@Override
	public StandalonePojoAssertionHelper assertions() {
		return assertionHelper;
	}

	@Override
	protected SetupVariant defaultSetupVariant() {
		return SetupVariant.variant();
	}

	@Override
	protected SetupContext createSetupContext(SetupVariant setupVariant) {
		return new SetupContext( schemaManagementStrategyName );
	}

	@Override
	protected void close(CloseableSearchMapping toClose) {
		toClose.close();
	}

	public static class SetupVariant {
		private static final SetupVariant INSTANCE = new SetupVariant();

		public static SetupVariant variant() {
			return INSTANCE;
		}

		protected SetupVariant() {
		}
	}

	public final class SetupContext
			extends
			MappingSetupHelper<SetupContext,
					SearchMappingBuilder,
					StandalonePojoMappingConfigurationContext,
					CloseableSearchMapping,
					SetupVariant>.AbstractSetupContext {
		// Use a LinkedHashSet/LinkedHashMap for deterministic iteration
		private final Set<Class<?>> annotatedTypes = new LinkedHashSet<>();
		private final Map<String, Object> properties = new LinkedHashMap<>();

		// Disable the bean-manager-based bean provider by default,
		// so that we detect code that relies on beans from the bean manager
		// whereas it should rely on reflection or built-in beans.
		private BeanProvider beanManagerBeanProvider = new ForbiddenBeanProvider();

		SetupContext(SchemaManagementStrategyName schemaManagementStrategyName) {
			properties.put( StandalonePojoMapperSettings.SCHEMA_MANAGEMENT_STRATEGY, schemaManagementStrategyName );
			// Ensure we don't build Jandex indexes needlessly:
			// discovery based on Jandex ought to be tested in real projects that don't use this setup helper.
			withConfiguration( builder -> builder.annotationMapping().buildMissingDiscoveredJandexIndexes( false ) );
			if ( defaultReindexOnUpdate != null ) {
				withConfiguration( builder -> builder.defaultReindexOnUpdate( defaultReindexOnUpdate ) );
			}
		}

		@Override
		public SetupContext withProperty(String key, Object value) {
			if ( value != null ) {
				properties.put( key, value );
			}
			else {
				properties.remove( key );
			}
			return thisAsC();
		}

		public SetupContext expectCustomBeans() {
			beanManagerBeanProvider = null;
			return thisAsC();
		}

		public SetupContext withAnnotatedTypes(Class<?>... annotatedEntityTypes) {
			return withAnnotatedTypes( CollectionHelper.asLinkedHashSet( annotatedEntityTypes ) );
		}

		public SetupContext withAnnotatedTypes(Set<Class<?>> annotatedTypes) {
			this.annotatedTypes.addAll( annotatedTypes );
			return this;
		}

		public SearchMapping setup(Class<?>... annotatedEntityTypes) {
			return withAnnotatedTypes( annotatedEntityTypes )
					.withConfiguration( builder -> {
						for ( Class<?> annotatedEntityType : annotatedEntityTypes ) {
							builder.programmaticMapping().type( annotatedEntityType )
									.searchEntity();
						}
					} )
					.setup();
		}

		@Override
		protected SearchMappingBuilder createBuilder() {
			return SearchMapping.builder( AnnotatedTypeSource.fromClasses( annotatedTypes ), lookup )
					.property( StandalonePojoMapperSpiSettings.BEAN_PROVIDER, beanManagerBeanProvider )
					.properties( properties );
		}

		@Override
		protected void consumeBeforeBuildConfigurations(SearchMappingBuilder builder,
				List<Consumer<StandalonePojoMappingConfigurationContext>> consumers) {
			List<Object> configurers = consumers.stream()
					.map( c -> (StandalonePojoMappingConfigurer) c::accept )
					.collect( Collectors.toList() );
			Object userConfigurers = properties.get( StandalonePojoMapperSettings.MAPPING_CONFIGURER );
			if ( userConfigurers != null ) {
				if ( userConfigurers instanceof Iterable ) {
					for ( Object userConfigurer : (Iterable<?>) userConfigurers ) {
						configurers.add( userConfigurer );
					}
				}
				else {
					configurers.add( userConfigurers );
				}
			}
			builder.property(
					StandalonePojoMapperSettings.MAPPING_CONFIGURER,
					configurers
			);
		}

		@Override
		protected CloseableSearchMapping build(SearchMappingBuilder builder) {
			return builder.build();
		}

		@Override
		protected BackendMappingHandle toBackendMappingHandle(CloseableSearchMapping result) {
			return new StandalonePojoMappingHandle();
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}
	}
}
