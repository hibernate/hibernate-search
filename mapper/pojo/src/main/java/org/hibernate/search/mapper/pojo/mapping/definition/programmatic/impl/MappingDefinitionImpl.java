/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperImplementor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;

/**
 * @author Yoann Rodiere
 */
public class MappingDefinitionImpl implements MappingDefinition {

	private final PojoMapperImplementor mappingType;

	private final Map<Class<?>, TypeMappingContextImpl> entities = new HashMap<>();

	public MappingDefinitionImpl(PojoMapperImplementor mappingType) {
		super();
		this.mappingType = mappingType;
	}

	@Override
	public void contribute(TypeMetadataCollector collector) {
		for ( TypeMappingContextImpl contextImpl : entities.values() ) {
			contextImpl.contribute( collector );
		}
	}

	@Override
	public TypeMappingContext type(Class<?> clazz) {
		return entities.computeIfAbsent( clazz, c -> new TypeMappingContextImpl( mappingType, c ) );
	}

}
