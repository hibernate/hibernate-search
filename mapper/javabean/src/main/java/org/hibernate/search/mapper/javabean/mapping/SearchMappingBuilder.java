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

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.mapper.javabean.impl.JavaBeanMappingInitiator;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingImpl;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingKey;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public final class SearchMappingBuilder {

	private static ConfigurationPropertySource getPropertySource(Map<String, Object> properties,
			ConfigurationPropertyChecker propertyChecker) {
		return propertyChecker.wrap( ConfigurationPropertySource.fromMap( properties ) );
	}

	private final ConfigurationPropertyChecker propertyChecker;
	private final Map<String, Object> properties = new HashMap<>();
	private final ConfigurationPropertySource propertySource;
	private final SearchIntegrationBuilder integrationBuilder;
	private final JavaBeanMappingKey mappingKey;
	private final JavaBeanMappingInitiator mappingInitiator;

	SearchMappingBuilder(MethodHandles.Lookup lookup) {
		propertyChecker = ConfigurationPropertyChecker.create();
		propertySource = getPropertySource( properties, propertyChecker );
		integrationBuilder = SearchIntegration.builder( propertySource, propertyChecker );
		JavaBeanBootstrapIntrospector introspector = JavaBeanBootstrapIntrospector.create( lookup );
		mappingKey = new JavaBeanMappingKey();
		mappingInitiator = new JavaBeanMappingInitiator( introspector );
		integrationBuilder.addMappingInitiator( mappingKey, mappingInitiator );
		// Enable annotated type discovery by default
		mappingInitiator.setAnnotatedTypeDiscoveryEnabled( true );
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

	/**
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#getMainWorkPlan() work plan}.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder addEntityType(Class<?> type) {
		mappingInitiator.addEntityType( type );
		return this;
	}

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link SearchSession#getMainWorkPlan() work plan}.
	 * @return {@code this}, for call chaining.
	 */
	public SearchMappingBuilder addEntityTypes(Set<Class<?>> types) {
		for ( Class<?> type : types ) {
			addEntityType( type );
		}
		return this;
	}

	public SearchMappingBuilder setMultiTenancyEnabled(boolean multiTenancyEnabled) {
		mappingInitiator.setMultiTenancyEnabled( multiTenancyEnabled );
		return this;
	}

	public SearchMappingBuilder setImplicitProvidedId(boolean multiTenancyEnabled) {
		mappingInitiator.setImplicitProvidedId( multiTenancyEnabled );
		return this;
	}

	public SearchMappingBuilder setAnnotatedTypeDiscoveryEnabled(boolean annotatedTypeDiscoveryEnabled) {
		mappingInitiator.setAnnotatedTypeDiscoveryEnabled( annotatedTypeDiscoveryEnabled );
		return this;
	}

	public SearchMappingBuilder setProperty(String name, Object value) {
		properties.put( name, value );
		return this;
	}

	public SearchMappingBuilder setProperties(Map<String, Object> map) {
		properties.putAll( map );
		return this;
	}

	public CloseableSearchMapping build() {
		SearchIntegrationPartialBuildState integrationPartialBuildState = integrationBuilder.prepareBuild();
		SearchIntegration integration = null;
		SearchMapping mapping;
		try {
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
					.push( SearchIntegrationPartialBuildState::closeOnFailure, integrationPartialBuildState );
			throw e;
		}

		try {
			/*
			 * Since the user doesn't have access to the integration, but only to the (closeable) mapping,
			 * make sure to close the integration whenever the mapping is closed by the user.
			 */
			JavaBeanMappingImpl mappingImpl = (JavaBeanMappingImpl) mapping;
			mappingImpl.onClose( integration::close );
			return mappingImpl;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( integration );
			throw e;
		}
	}
}
