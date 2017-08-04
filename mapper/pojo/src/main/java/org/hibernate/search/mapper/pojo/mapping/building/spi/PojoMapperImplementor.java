/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.engine.common.SearchManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperImplementor;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuilder;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;


/**
 * @author Yoann Rodiere
 */
public abstract class PojoMapperImplementor
		implements MapperImplementor<PojoTypeNodeMappingCollector, PojoSearchManager, SearchManagerBuilder<PojoSearchManager>> {

	private final PojoIntrospector introspector;
	private final PojoProxyIntrospector proxyIntrospector;
	private final boolean implicitProvidedId;

	protected PojoMapperImplementor(PojoIntrospector introspector, PojoProxyIntrospector proxyIntrospector, boolean implicitProvidedId) {
		this.introspector = introspector;
		this.proxyIntrospector = proxyIntrospector;
		this.implicitProvidedId = implicitProvidedId;
	}

	@Override
	public IndexableTypeOrdering getTypeOrdering() {
		return PojoTypeOrdering.get();
	}

	@Override
	public MappingBuilder<PojoTypeNodeMappingCollector, SearchManagerBuilder<PojoSearchManager>> createBuilder() {
		return new PojoMappingBuilder( introspector, proxyIntrospector, implicitProvidedId );
	}

}
