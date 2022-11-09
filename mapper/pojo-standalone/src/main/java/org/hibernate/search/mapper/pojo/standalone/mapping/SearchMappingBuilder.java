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

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.pojo.standalone.cfg.spi.StandalonePojoMapperSpiSettings;
import org.hibernate.search.mapper.pojo.standalone.impl.StandalonePojoMappingInitiator;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.StandalonePojoMappingKey;
import org.hibernate.search.mapper.pojo.standalone.model.impl.StandalonePojoBootstrapIntrospector;
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
