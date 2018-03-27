/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public class ProgrammaticMappingDefinitionImpl implements ProgrammaticMappingDefinition, MetadataContributor {

	private final MapperFactory<PojoTypeMetadataContributor, ?> mapperFactory;

	private final PojoBootstrapIntrospector introspector;

	private final Map<Class<?>, TypeMappingContextImpl> entities = new HashMap<>();

	public ProgrammaticMappingDefinitionImpl(MapperFactory<PojoTypeMetadataContributor, ?> mapperFactory,
			PojoBootstrapIntrospector introspector) {
		this.mapperFactory = mapperFactory;
		this.introspector = introspector;
	}

	@Override
	public void contribute(BuildContext buildContext, MetadataCollector collector) {
		for ( TypeMappingContextImpl contextImpl : entities.values() ) {
			contextImpl.contribute( buildContext, collector );
		}
	}

	@Override
	public TypeMappingContext type(Class<?> clazz) {
		return entities.computeIfAbsent( clazz, c -> new TypeMappingContextImpl( mapperFactory, introspector.getTypeModel( c ) ) );
	}

}
