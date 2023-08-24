/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
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
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

public final class StandalonePojoMappingSetupHelper
		extends
		MappingSetupHelper<StandalonePojoMappingSetupHelper.SetupContext,
				SearchMappingBuilder,
				StandalonePojoMappingConfigurationContext,
				CloseableSearchMapping> {

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
	protected SetupContext createSetupContext() {
		return new SetupContext( schemaManagementStrategyName );
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
			return SearchMapping.builder( lookup )
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
