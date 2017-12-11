/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMapper;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;


/**
 * @author Yoann Rodiere
 */
public abstract class PojoMapperFactory<M extends MappingImplementor>
		implements MapperFactory<PojoTypeNodeMetadataContributor, M> {

	private final PojoIntrospector introspector;
	private final boolean implicitProvidedId;

	protected PojoMapperFactory(PojoIntrospector introspector, boolean implicitProvidedId) {
		this.introspector = introspector;
		this.implicitProvidedId = implicitProvidedId;
	}

	@Override
	public IndexableTypeOrdering getTypeOrdering() {
		return PojoTypeOrdering.get();
	}

	@Override
	public final PojoMapper<M> createMapper(BuildContext buildContext, ConfigurationPropertySource propertySource) {
		return new PojoMapper<>( buildContext, propertySource, introspector, implicitProvidedId, this::createMapping );
	}

	protected abstract M createMapping(ConfigurationPropertySource propertySource,
			PojoMappingDelegate mappingDelegate);

}
