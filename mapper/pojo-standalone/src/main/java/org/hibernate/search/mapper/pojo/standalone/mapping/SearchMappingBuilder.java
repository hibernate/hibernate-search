/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping;

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
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.impl.StandalonePojoMappingInitiator;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMappingKey;
import org.hibernate.search.mapper.pojo.standalone.mapping.metadata.EntityConfigurer;
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgesConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public final class SearchMappingBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final OptionalConfigurationProperty<BeanProvider> BEAN_PROVIDER =
			ConfigurationProperty.forKey( StandalonePojoMapperSpiSettings.BEAN_PROVIDER )
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
	private final StandalonePojoMappingKey mappingKey;
	private final StandalonePojoMappingInitiator mappingInitiator;

	SearchMappingBuilder(MethodHandles.Lookup lookup) {
		propertyChecker = ConfigurationPropertyChecker.create();
		propertySource = getPropertySource( properties, propertyChecker );
		StandalonePojoBootstrapIntrospector introspector = StandalonePojoBootstrapIntrospector.create( lookup );
		mappingKey = new StandalonePojoMappingKey();
		mappingInitiator = new StandalonePojoMappingInitiator( introspector );
		// Enable annotated type discovery by default
		mappingInitiator.annotationMapping()
				.discoverAnnotatedTypesFromRootMappingAnnotations( true )
				.discoverJandexIndexesFromAddedTypes( true )
				.discoverAnnotationsFromReferencedTypes( true );
	}

	/**
	 * Starts the definition of the programmatic mapping.
	 * @return A context to define the programmatic mapping.
	 */
	public ProgrammaticMappingConfigurationContext programmaticMapping() {
		return mappingInitiator.programmaticMapping();
	}

	/**
	 * Starts the definition of the annotation mapping.
	 * @return A context to define the annotation mapping.
	 */
	public AnnotationMappingConfigurationContext annotationMapping() {
		return mappingInitiator.annotationMapping();
	}

	/**
	 * Starts the definition of container extractors available for use in mappings.
	 * @return A context to define container extractors.
	 */
	public ContainerExtractorConfigurationContext containerExtractors() {
		return mappingInitiator.containerExtractors();
	}

	/**
	 * Starts the definition of bridges to apply by default in mappings.
	 * @return A context to define default bridges.
	 */
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

	/**
	 * Enables or disables multi-tenancy.
	 * <p>
	 * If multi-tenancy is enabled, every {@link SearchMapping#createSession() session} will need to be assigned a tenant identifier.
	 *
	 * @param multiTenancyEnabled {@code true} to enable multi-tenancy, {@code false} to disable it (the default).
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder multiTenancyEnabled(boolean multiTenancyEnabled) {
		mappingInitiator.tenancyMode( multiTenancyEnabled ? TenancyMode.MULTI_TENANCY : TenancyMode.SINGLE_TENANCY );
		return this;
	}

	/**
	 * Defines the default depth of automatic reindexing.
	 * <p>
	 * Keep the default value ({@link ReindexOnUpdate#DEFAULT} if your entity model is a graph (normalized model, e.g. in ORMs);
	 * pass {@link ReindexOnUpdate#SHALLOW} if your entity model is a collection of trees (denormalized model, e.g. in a document datastore).
	 * <p>
	 * The exact behavior is as follows:
	 * <ul>
	 *     <li>If set to {@link ReindexOnUpdate#DEFAULT}, when entity A is {@link IndexedEmbedded indexed-embedded} in entity B,
	 *     and a relevant property of entity A changes, then Hibernate Search will trigger reindexing of
	 *     entity A (if appropriate) <em>and</em> entity B (if appropriate).</li>
	 *     <li>If set to {@link ReindexOnUpdate#SHALLOW}, when entity A is {@link IndexedEmbedded indexed-embedded} in entity B,
	 *     and a relevant property of entity A changes, then Hibernate Search will trigger reindexing of
	 *     entity A (if appropriate) <em>only</em>, and not entity B.</li>
	 *     <li>If set to {@link ReindexOnUpdate#NO}, when entity A is {@link IndexedEmbedded indexed-embedded} in entity B,
	 *     and a relevant property of entity A changes, then Hibernate Search will trigger reindexing of
	 *     neither entity A nor entity B. The only way to trigger reindexing will be to force it,
	 *     e.g. with {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object)}
	 *     (without passing a list of dirty paths)
	 *     or {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object, DocumentRoutesDescriptor, Object, boolean, boolean, String...)}
	 *     (with {@code forceSelfDirty = true} or {@code forceContainedDirty = true}) </li>
	 * </ul>
	 *
	 * @param defaultReindexOnUpdate The default behavior
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder defaultReindexOnUpdate(ReindexOnUpdate defaultReindexOnUpdate) {
		mappingInitiator.defaultReindexOnUpdate( defaultReindexOnUpdate );
		return this;
	}

	/**
	 * @param providedIdentifierBridge An identifier bridge to use by default for entities that don't have a property annotated
	 * with {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId}.
	 * Caution: the bridge will be applied to the whole entity, with the expectation that the identifier never changes for a given entity.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder providedIdentifierBridge(BeanReference<? extends IdentifierBridge<Object>> providedIdentifierBridge) {
		mappingInitiator.providedIdentifierBridge( providedIdentifierBridge );
		return this;
	}

	/**
	 * Sets a configuration property.
	 * <p>
	 * Configuration properties are mentioned in {@link org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings},
	 * or in the reference documentation for backend-related properties.
	 *
	 * @param name The name (key) of the configuration property.
	 * @param value The value of the configuration property.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder property(String name, Object value) {
		properties.put( name, value );
		return this;
	}

	/**
	 * Sets multiple configuration properties.
	 * <p>
	 * Configuration properties are mentioned in {@link org.hibernate.search.mapper.pojo.standalone.cfg.StandalonePojoMapperSettings},
	 * or in the reference documentation for backend-related properties.
	 *
	 * @param map A map containing property names (property keys) as map keys and property values as map values.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder properties(Map<String, ?> map) {
		properties.putAll( map );
		return this;
	}

	/**
	 * Builds the search mapping.
	 * @return The {@link SearchMapping}.
	 */
	public CloseableSearchMapping build() {
		SearchIntegrationEnvironment environment = null;
		SearchIntegrationPartialBuildState integrationPartialBuildState = null;
		StandalonePojoMapping mapping = null;
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
			finalizer.finalizeIntegration();
			return mapping;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( StandalonePojoMapping::close, mapping )
					.push( environment )
					.push( SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildState );
			throw e;
		}
	}
}
