/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.Set;

import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;
import org.hibernate.search.mapper.pojo.mapping.PojoMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.MappingDefinitionImpl;

/**
 * @author Yoann Rodiere
 */
public abstract class PojoMappingContributorImpl<M extends PojoMapping, MI extends MappingImplementor>
		implements PojoMappingContributor<M> {

	private final SearchMappingRepositoryBuilder mappingRepositoryBuilder;

	private final PojoMapperFactory<MI> mapperFactory;

	protected PojoMappingContributorImpl(
			SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			PojoMapperFactory<MI> mapperFactory) {
		this.mappingRepositoryBuilder = mappingRepositoryBuilder;
		this.mapperFactory = mapperFactory;
	}

	@Override
	public MappingDefinition programmaticMapping() {
		MappingDefinitionImpl definition = new MappingDefinitionImpl( mapperFactory );
		mappingRepositoryBuilder.addMapping( definition );
		return definition;
	}

	@Override
	public void annotationMapping(Set<Class<?>> classes) {
		// TODO Annotation processing
		throw new UnsupportedOperationException( "Annotation processing is not implemented yet" );
	}

	@Override
	public M getResult() {
		return toReturnType( mappingRepositoryBuilder.getBuiltResult().getMapping( mapperFactory ) );
	}

	protected abstract M toReturnType(MI mapping);
}
