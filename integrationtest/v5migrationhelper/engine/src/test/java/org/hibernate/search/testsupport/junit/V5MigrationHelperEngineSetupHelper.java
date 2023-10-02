/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.junit;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMappingBuilder;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.testsupport.configuration.V5MigrationHelperTestLuceneBackendConfiguration;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

public final class V5MigrationHelperEngineSetupHelper
		extends
		MappingSetupHelper<V5MigrationHelperEngineSetupHelper.SetupContext,
				SearchMappingBuilder,
				StandalonePojoMappingConfigurationContext,
				CloseableSearchMapping,
				V5MigrationHelperEngineSetupHelper.SetupVariant> {

	public static V5MigrationHelperEngineSetupHelper create() {
		return new V5MigrationHelperEngineSetupHelper(
				BackendSetupStrategy.withSingleBackend( new V5MigrationHelperTestLuceneBackendConfiguration() )
		);
	}

	private final StandalonePojoAssertionHelper assertionHelper;

	private V5MigrationHelperEngineSetupHelper(BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
		this.assertionHelper = new StandalonePojoAssertionHelper( backendSetupStrategy );
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
	protected SetupContext createSetupContext(SetupVariant variant) {
		return new SetupContext();
	}

	@Override
	protected void close(CloseableSearchMapping toClose) {
		toClose.close();
	}

	public static final class SetupVariant extends StandalonePojoMappingSetupHelper.SetupVariant {
		private static final SetupVariant INSTANCE = new SetupVariant();

		public static SetupVariant variant() {
			return INSTANCE;
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

		SetupContext() {
			// Ensure we don't build Jandex indexes needlessly:
			// discovery based on Jandex ought to be tested in real projects that don't use this setup helper.
			withConfiguration( builder -> builder.annotationMapping().buildMissingDiscoveredJandexIndexes( false ) );
		}

		@Override
		public SetupContext withProperty(String key, Object value) {
			properties.put( key, value );
			return thisAsC();
		}

		public SetupContext withAnnotatedTypes(Class<?>... annotatedEntityTypes) {
			return withAnnotatedTypes( CollectionHelper.asLinkedHashSet( annotatedEntityTypes ) );
		}

		public SetupContext withAnnotatedTypes(Set<Class<?>> annotatedTypes) {
			annotatedTypes.addAll( annotatedTypes );
			return this;
		}

		public SearchMapping setup(Class<?>... annotatedEntityTypes) {
			return withAnnotatedTypes( annotatedEntityTypes )
					.withConfiguration( builder -> {
						for ( Class<?> type : annotatedEntityTypes ) {
							builder.programmaticMapping().type( type )
									.searchEntity();
						}
					} )
					.setup();
		}

		@Override
		protected SearchMappingBuilder createBuilder() {
			return SearchMapping.builder( AnnotatedTypeSource.fromClasses( annotatedTypes ) ).properties( properties );
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
			return new V5MigrationHelperEngineMappingHandle();
		}

		@Override
		protected SetupContext thisAsC() {
			return this;
		}
	}

}
