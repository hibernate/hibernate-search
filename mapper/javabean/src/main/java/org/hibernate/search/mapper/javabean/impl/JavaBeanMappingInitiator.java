/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMapperDelegate;
import org.hibernate.search.mapper.javabean.mapping.impl.JavaBeanMappingPartialBuildState;
import org.hibernate.search.mapper.javabean.mapping.metadata.EntityConfigurer;
import org.hibernate.search.mapper.javabean.mapping.metadata.impl.JavaBeanEntityTypeMetadataProvider;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingInitiator;

public class JavaBeanMappingInitiator extends AbstractPojoMappingInitiator<JavaBeanMappingPartialBuildState> {

	private final JavaBeanEntityTypeMetadataProvider.Builder entityTypeMetadataProviderBuilder;

	private JavaBeanEntityTypeMetadataProvider entityTypeMetadataProvider;

	public JavaBeanMappingInitiator(JavaBeanBootstrapIntrospector introspector) {
		super( introspector );
		entityTypeMetadataProviderBuilder = new JavaBeanEntityTypeMetadataProvider.Builder( introspector );
	}

	public <E> void addEntityType(Class<E> clazz, String entityName, EntityConfigurer<E> configurerOrNull) {
		entityTypeMetadataProviderBuilder.addEntityType( clazz, entityName, configurerOrNull );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		entityTypeMetadataProvider = entityTypeMetadataProviderBuilder.build();
		addConfigurationContributor( new JavaBeanTypeConfigurationContributor( entityTypeMetadataProvider ) );
		super.configure( buildContext, configurationCollector );
	}

	@Override
	protected PojoMapperDelegate<JavaBeanMappingPartialBuildState> createMapperDelegate() {
		return new JavaBeanMapperDelegate( entityTypeMetadataProvider );
	}
}
