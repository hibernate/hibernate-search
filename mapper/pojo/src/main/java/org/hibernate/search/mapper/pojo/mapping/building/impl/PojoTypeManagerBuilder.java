/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.bridge.spi.IdentifierBridge;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManager;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.processing.impl.IdentifierConverter;
import org.hibernate.search.mapper.pojo.processing.impl.PojoTypeNodeProcessorBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PropertyIdentifierConverter;
import org.hibernate.search.util.SearchException;

public class PojoTypeManagerBuilder<E, D extends DocumentState> {
	private final PojoProxyIntrospector proxyIntrospector;
	private final Class<E> javaType;
	private final IndexManagerBuildingState<D> indexManagerBuildingState;

	private final PojoTypeNodeProcessorBuilder processorBuilder;
	private IdentifierConverter<?, E> idConverter;

	public PojoTypeManagerBuilder(Class<E> javaType,
			PojoIntrospector introspector, PojoProxyIntrospector proxyIntrospector,
			IndexManagerBuildingState<D> indexManagerBuildingState,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			IdentifierConverter<?, E> defaultIdentifierConverter) {
		this.proxyIntrospector = proxyIntrospector;
		this.javaType = javaType;
		this.indexManagerBuildingState = indexManagerBuildingState;
		this.processorBuilder = new PojoTypeNodeProcessorBuilder(
				javaType, introspector, contributorProvider,
				indexManagerBuildingState.getModelCollector(),
				this::setIdentifierBridge );
		this.idConverter = defaultIdentifierConverter;
	}

	public PojoTypeNodeMappingCollector asCollector() {
		return processorBuilder;
	}

	private void setIdentifierBridge(PropertyHandle handle, IdentifierBridge<?> bridge) {
		this.idConverter = new PropertyIdentifierConverter<>( handle, bridge );
	}

	public PojoTypeManager<?, E, D> build() {
		if ( idConverter == null ) {
			throw new SearchException( "Missing identifier mapping for indexed type '" + javaType + "'" );
		}
		return new PojoTypeManager<>( proxyIntrospector, idConverter, javaType,
				processorBuilder.build(), indexManagerBuildingState.getResult() );
	}
}