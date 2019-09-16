/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class JavaBeanTypeConfigurationContributor implements PojoMappingConfigurationContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanBootstrapIntrospector introspector;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<Class<?>, String> entityNameByType = new LinkedHashMap<>();
	private final Map<String, Class<?>> entityTypeByName = new LinkedHashMap<>();

	public JavaBeanTypeConfigurationContributor(JavaBeanBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		for ( Map.Entry<Class<?>, String> entry : entityNameByType.entrySet() ) {
			PojoRawTypeModel<?> typeModel = introspector.getTypeModel( entry.getKey() );
			configurationCollector.collectContributor(
					typeModel,
					new JavaBeanEntityTypeContributor( typeModel, entry.getValue() )
			);
		}
	}

	void addEntityType(Class<?> type, String entityName) {
		entityNameByType.put( type, entityName );
		Class<?> previousType = entityTypeByName.putIfAbsent( entityName, type );
		if ( previousType != null && !previousType.equals( type ) ) {
			throw log.multipleEntityTypesWithSameName( entityName, previousType, type );
		}
	}
}
