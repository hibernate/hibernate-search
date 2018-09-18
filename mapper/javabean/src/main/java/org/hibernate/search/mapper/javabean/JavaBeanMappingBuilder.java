/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.mapper.javabean.impl.JavaBeanMappingInitiatorImpl;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingImpl;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingKey;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.util.impl.common.SuppressingCloser;

public final class JavaBeanMappingBuilder {

	private final SearchIntegrationBuilder integrationBuilder;
	private final JavaBeanMappingKey mappingKey;
	private final JavaBeanMappingInitiatorImpl mappingInitiator;

	JavaBeanMappingBuilder(ConfigurationPropertySource propertySource, MethodHandles.Lookup lookup) {
		integrationBuilder = SearchIntegration.builder( propertySource );
		JavaBeanBootstrapIntrospector introspector = new JavaBeanBootstrapIntrospector( lookup );
		mappingKey = new JavaBeanMappingKey();
		mappingInitiator = new JavaBeanMappingInitiatorImpl( introspector );
		integrationBuilder.addMappingInitiator( mappingKey, mappingInitiator );
		// Enable annotated type discovery by default
		mappingInitiator.setAnnotatedTypeDiscoveryEnabled( true );
	}

	public ProgrammaticMappingDefinition programmaticMapping() {
		return mappingInitiator.programmaticMapping();
	}

	public AnnotationMappingDefinition annotationMapping() {
		return mappingInitiator.annotationMapping();
	}

	/**
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link org.hibernate.search.mapper.pojo.mapping.PojoWorkPlan}.
	 * @return {@code this}, for call chaining.
	 */
	public JavaBeanMappingBuilder addEntityType(Class<?> type) {
		mappingInitiator.addEntityType( type );
		return this;
	}

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link org.hibernate.search.mapper.pojo.mapping.PojoWorkPlan}.
	 * @return {@code this}, for call chaining.
	 */
	public JavaBeanMappingBuilder addEntityTypes(Set<Class<?>> types) {
		for ( Class<?> type : types ) {
			addEntityType( type );
		}
		return this;
	}

	public JavaBeanMappingBuilder setMultiTenancyEnabled(boolean multiTenancyEnabled) {
		mappingInitiator.setMultiTenancyEnabled( multiTenancyEnabled );
		return this;
	}

	public JavaBeanMappingBuilder setAnnotatedTypeDiscoveryEnabled(boolean annotatedTypeDiscoveryEnabled) {
		mappingInitiator.setAnnotatedTypeDiscoveryEnabled( annotatedTypeDiscoveryEnabled );
		return this;
	}

	public JavaBeanMappingBuilder setProperty(String name, String value) {
		integrationBuilder.setProperty( name, value );
		return this;
	}

	public CloseableJavaBeanMapping build() {
		SearchIntegration integration = integrationBuilder.build();
		try {
			JavaBeanMapping mapping = integration.getMapping( mappingKey );

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
