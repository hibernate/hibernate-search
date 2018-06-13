/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;

class JavaBeanTypeConfigurationContributor implements PojoMappingConfigurationContributor {

	private final JavaBeanBootstrapIntrospector introspector;

	// Use a LinkedHashSet for deterministic iteration
	private final Set<Class<?>> entityTypes = new LinkedHashSet<>();

	public JavaBeanTypeConfigurationContributor(JavaBeanBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public void configure(BuildContext buildContext, ConfigurationPropertySource propertySource,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		for ( Class<?> type : entityTypes ) {
			configurationCollector.collectContributor(
					introspector.getTypeModel( type ),
					new JavaBeanEntityTypeContributor()
			);
		}
	}

	void addEntityType(Class<?> type) {
		entityTypes.add( type );
	}
}
