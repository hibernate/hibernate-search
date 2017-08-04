/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.Set;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingContributor;
import org.hibernate.search.mapper.pojo.mapping.PojoMapper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperImplementor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.MappingDefinitionImpl;

/**
 * @author Yoann Rodiere
 */
public abstract class PojoMapperImpl implements PojoMapper {

	private final PojoMapperImplementor implementor;

	protected PojoMapperImpl(PojoMapperImplementor mappingType) {
		this.implementor = mappingType;
	}

	@Override
	public MappingDefinition programmaticMapping() {
		return new MappingDefinitionImpl( implementor );
	}

	@Override
	public MappingContributor<?> annotationMapping(Set<Class<?>> classes) {
		// TODO Annotation processing
		throw new UnsupportedOperationException( "Annotation processing is not implemented yet" );
	}

}
