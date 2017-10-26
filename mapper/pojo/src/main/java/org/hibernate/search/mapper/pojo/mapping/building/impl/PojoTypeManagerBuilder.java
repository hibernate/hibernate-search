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
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManagerContainer;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.IdentifierConverter;
import org.hibernate.search.mapper.pojo.processing.impl.PojoTypeNodeProcessorBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PropertyIdentifierConverter;
import org.hibernate.search.util.SearchException;

public class PojoTypeManagerBuilder<E, D extends DocumentState> {
	private final Class<E> javaType;
	private final IndexManagerBuildingState<D> indexManagerBuildingState;

	private final PojoTypeNodeProcessorBuilder processorBuilder;
	private IdentifierConverter<?, E> idConverter;

	public PojoTypeManagerBuilder(TypeModel<E> typeModel,
			IndexManagerBuildingState<D> indexManagerBuildingState,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			IdentifierConverter<?, E> defaultIdentifierConverter) {
		this.javaType = typeModel.getJavaType();
		this.indexManagerBuildingState = indexManagerBuildingState;
		this.processorBuilder = new PojoTypeNodeProcessorBuilder(
				typeModel, contributorProvider,
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

	public void addTo(PojoTypeManagerContainer.Builder builder) {
		if ( idConverter == null ) {
			throw new SearchException( "Missing identifier mapping for indexed type '" + javaType + "'" );
		}
		PojoTypeManager<?, E, D> typeManager = new PojoTypeManager<>( idConverter, javaType,
				processorBuilder.build(), indexManagerBuildingState.build() );
		builder.add( indexManagerBuildingState.getIndexName(), javaType, typeManager );
	}
}