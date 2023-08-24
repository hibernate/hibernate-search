/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.junit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMappingBuilder;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.testsupport.configuration.V5MigrationHelperTestLuceneBackendConfiguration;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoAssertionHelper;

public final class V5MigrationHelperEngineSetupHelper
		extends
		MappingSetupHelper<V5MigrationHelperEngineSetupHelper.SetupContext,
				SearchMappingBuilder,
				StandalonePojoMappingConfigurationContext,
				CloseableSearchMapping> {

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
	protected SetupContext createSetupContext() {
		return new SetupContext();
	}

	@Override
	protected void close(CloseableSearchMapping toClose) {
		toClose.close();
	}

	public final class SetupContext
			extends
			MappingSetupHelper<SetupContext,
					SearchMappingBuilder,
					StandalonePojoMappingConfigurationContext,
					CloseableSearchMapping>.AbstractSetupContext {

		// Use a LinkedHashMap for deterministic iteration
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

		public SetupContext withAnnotatedEntityType(Class<?> annotatedEntityType, String entityName) {
			return withConfiguration( builder -> {
				builder.addEntityType( annotatedEntityType, entityName );
				builder.annotationMapping().add( annotatedEntityType );
			} );
		}

		public SetupContext withAnnotatedEntityTypes(Class<?>... annotatedEntityTypes) {
			return withAnnotatedEntityTypes( CollectionHelper.asLinkedHashSet( annotatedEntityTypes ) );
		}

		public SetupContext withAnnotatedEntityTypes(Set<Class<?>> annotatedEntityTypes) {
			return withConfiguration( builder -> {
				builder.addEntityTypes( annotatedEntityTypes );
				builder.annotationMapping().add( annotatedEntityTypes );
			} );
		}

		public SetupContext withAnnotatedTypes(Class<?>... annotatedTypes) {
			return withAnnotatedTypes( CollectionHelper.asLinkedHashSet( annotatedTypes ) );
		}

		public SetupContext withAnnotatedTypes(Set<Class<?>> annotatedTypes) {
			return withConfiguration( builder -> builder.annotationMapping().add( annotatedTypes ) );
		}

		public SearchMapping setup(Class<?>... annotatedEntityTypes) {
			return withAnnotatedEntityTypes( annotatedEntityTypes ).setup();
		}

		@Override
		protected SearchMappingBuilder createBuilder() {
			return SearchMapping.builder().properties( properties );
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
