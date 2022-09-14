/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.javabean.cfg.spi.JavaBeanMapperSpiSettings;
import org.hibernate.search.mapper.javabean.impl.JavaBeanMappingInitiator;
import org.hibernate.search.mapper.javabean.logging.impl.Log;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingKey;
import org.hibernate.search.mapper.javabean.mapping.metadata.EntityConfigurer;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class SearchMappingBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<BeanProvider> BEAN_PROVIDER =
			ConfigurationProperty.forKey( JavaBeanMapperSpiSettings.BEAN_PROVIDER )
					.as( BeanProvider.class, value -> {
						throw log.invalidStringForBeanProvider( value, BeanProvider.class );
					} )
					.build();

	private static ConfigurationPropertySource getPropertySource(Map<String, Object> properties,
			ConfigurationPropertyChecker propertyChecker) {
		return propertyChecker.wrap( AllAwareConfigurationPropertySource.fromMap( properties ) );
	}

	private final ConfigurationPropertyChecker propertyChecker;
	private final Map<String, Object> properties = new HashMap<>();
	private final ConfigurationPropertySource propertySource;
	private final JavaBeanMappingKey mappingKey;
	private final JavaBeanMappingInitiator mappingInitiator;

	SearchMappingBuilder(MethodHandles.Lookup lookup) {
		propertyChecker = ConfigurationPropertyChecker.create();
		propertySource = getPropertySource( properties, propertyChecker );
		JavaBeanBootstrapIntrospector introspector = JavaBeanBootstrapIntrospector.create( lookup );
		mappingKey = new JavaBeanMappingKey();
		mappingInitiator = new JavaBeanMappingInitiator( introspector );
		// Enable annotated type discovery by default
		mappingInitiator.annotatedTypeDiscoveryEnabled( true );
	}

	public ProgrammaticMappingConfigurationContext programmaticMapping() {
		return mappingInitiator.programmaticMapping();
	}

	public AnnotationMappingConfigurationContext annotationMapping() {
		return mappingInitiator.annotationMapping();
	}

	public ContainerExtractorConfigurationContext containerExtractors() {
		return mappingInitiator.containerExtractors();
	}

	public BridgesConfigurationContext bridges() {
		return mappingInitiator.bridges();
	}

	/**
	 * Register a type as an entity type with the default name, its simple class name.
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder addEntityType(Class<?> type) {
		return addEntityType( type, type.getSimpleName() );
	}

	/**
	 * Register a type as an entity type with the given name.
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @param entityName The name of the entity.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder addEntityType(Class<?> type, String entityName) {
		return addEntityType( type, entityName, null );
	}

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder addEntityTypes(Set<Class<?>> types) {
		for ( Class<?> type : types ) {
			addEntityType( type );
		}
		return this;
	}

	/**
	 * Register a type as an entity type with the default name, its simple class name.
	 * @param <E> The entity type.
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @param configurer The configurer, to provide additional information about the entity type.
	 *
	 * @return {@code this}, for call chaining.
	 */
	public <E> SearchMappingBuilder addEntityType(Class<E> type, EntityConfigurer<E> configurer) {
		return addEntityType( type, type.getSimpleName(), configurer );
	}

	/**
	 * Register a type as an entity type with the given name.
	 * @param <E> The entity type.
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#indexingPlan() indexing plan}.
	 * @param entityName The name of the entity.
	 * @param configurer The configurer, to provide additional information about the entity type.
	 *
	 * @return {@code this}, for call chaining.
	 */
	public <E> SearchMappingBuilder addEntityType(Class<E> type, String entityName, EntityConfigurer<E> configurer) {
		mappingInitiator.addEntityType( type, entityName, configurer );
		return this;
	}

	public SearchMappingBuilder multiTenancyEnabled(boolean multiTenancyEnabled) {
		mappingInitiator.tenancyMode( multiTenancyEnabled ? TenancyMode.MULTI_TENANCY : TenancyMode.SINGLE_TENANCY );
		return this;
	}

	public void defaultReindexOnUpdate(ReindexOnUpdate defaultReindexOnUpdate) {
		mappingInitiator.defaultReindexOnUpdate( defaultReindexOnUpdate );
	}

	public SearchMappingBuilder providedIdentifierBridge(BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge) {
		mappingInitiator.providedIdentifierBridge( providedIdentifierBridge );
		return this;
	}

	public SearchMappingBuilder containedEntityIdentityMappingRequired(boolean required) {
		mappingInitiator.containedEntityIdentityMappingRequired( required );
		return this;
	}

	public SearchMappingBuilder annotatedTypeDiscoveryEnabled(boolean annotatedTypeDiscoveryEnabled) {
		mappingInitiator.annotatedTypeDiscoveryEnabled( annotatedTypeDiscoveryEnabled );
		return this;
	}

	public SearchMappingBuilder property(String name, Object value) {
		properties.put( name, value );
		return this;
	}

	public SearchMappingBuilder properties(Map<String, Object> map) {
		properties.putAll( map );
		return this;
	}

	public CloseableSearchMapping build() {
		SearchIntegrationEnvironment environment = null;
		SearchIntegrationPartialBuildState integrationPartialBuildState = null;
		SearchIntegration integration;
		SearchMapping mapping;
		try {
			SearchIntegrationEnvironment.Builder environmentBuilder =
					SearchIntegrationEnvironment.builder( propertySource, propertyChecker );
			BEAN_PROVIDER.get( propertySource ).ifPresent( environmentBuilder::beanProvider );
			environment = environmentBuilder.build();

			SearchIntegration.Builder integrationBuilder = SearchIntegration.builder( environment );
			integrationBuilder.addMappingInitiator( mappingKey, mappingInitiator );

			integrationPartialBuildState = integrationBuilder.prepareBuild();

			SearchIntegrationFinalizer finalizer =
					integrationPartialBuildState.finalizer( propertySource, propertyChecker );
			mapping = finalizer.finalizeMapping(
					mappingKey,
					(context, partialMapping) -> partialMapping.finalizeMapping()
			);
			integration = finalizer.finalizeIntegration();
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( environment )
					.push( SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildState );
			throw e;
		}

		try {
			/*
			 * Since the user doesn't have access to the integration, but only to the (closeable) mapping,
			 * make sure to close the integration whenever the mapping is closed by the user.
			 */
			JavaBeanMapping mappingImpl = (JavaBeanMapping) mapping;
			mappingImpl.setIntegration( integration );
			return mappingImpl;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( integration );
			throw e;
		}
	}
}
