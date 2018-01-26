/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManagerContainer;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.IdentifierMapping;
import org.hibernate.search.mapper.pojo.processing.impl.PojoTypeNodeProcessorBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PropertyIdentifierMapping;
import org.hibernate.search.mapper.pojo.processing.impl.RoutingKeyBridgeProvider;
import org.hibernate.search.mapper.pojo.processing.impl.RoutingKeyProvider;
import org.hibernate.search.util.SearchException;

public class PojoTypeManagerBuilder<E, D extends DocumentElement> {
	private final Class<E> javaType;
	private final IndexManagerBuildingState<D> indexManagerBuildingState;

	private final PojoTypeNodeIdentityMappingCollectorImpl identityMappingCollector;
	private final PojoTypeNodeProcessorBuilder processorBuilder;

	public PojoTypeManagerBuilder(TypeModel<E> typeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder,
			IndexManagerBuildingState<D> indexManagerBuildingState,
			IdentifierMapping<?, E> defaultIdentifierMapping) {
		this.javaType = typeModel.getJavaType();
		this.indexManagerBuildingState = indexManagerBuildingState;
		this.identityMappingCollector = new PojoTypeNodeIdentityMappingCollectorImpl( defaultIdentifierMapping );
		IndexModelBindingContext bindingContext = indexManagerBuildingState.getRootBindingContext();
		this.processorBuilder = new PojoTypeNodeProcessorBuilder(
				null, typeModel, contributorProvider, indexModelBinder, bindingContext, identityMappingCollector
		);
	}

	public PojoTypeNodeMappingCollector asCollector() {
		return processorBuilder;
	}

	public void addTo(PojoTypeManagerContainer.Builder builder) {
		IdentifierMapping<?, E> identifierMapping = identityMappingCollector.identifierMapping;
		if ( identifierMapping == null ) {
			throw new SearchException( "Missing identifier mapping for indexed type '" + javaType + "'" );
		}
		RoutingKeyBridge routingKeyBridge = identityMappingCollector.routingKeyBridge;
		RoutingKeyProvider<E> routingKeyProvider;
		if ( routingKeyBridge == null ) {
			routingKeyProvider = RoutingKeyProvider.alwaysNull();
		}
		else {
			routingKeyProvider = new RoutingKeyBridgeProvider<>( routingKeyBridge );
		}
		PojoTypeManager<?, E, D> typeManager = new PojoTypeManager<>( javaType,
				identifierMapping, routingKeyProvider,
				processorBuilder.build(), indexManagerBuildingState.build() );
		builder.add( indexManagerBuildingState.getIndexName(), javaType, typeManager );
	}

	private class PojoTypeNodeIdentityMappingCollectorImpl implements PojoTypeNodeIdentityMappingCollector {
		private IdentifierMapping<?, E> identifierMapping;
		private RoutingKeyBridge routingKeyBridge;

		public PojoTypeNodeIdentityMappingCollectorImpl(IdentifierMapping<?, E> identifierMapping) {
			this.identifierMapping = identifierMapping;
		}

		@Override
		public void identifierBridge(PropertyHandle handle, IdentifierBridge<?> bridge) {
			// FIXME ensure the bridge is closed upon build failure and when closing the SearchManagerRepository
			this.identifierMapping = new PropertyIdentifierMapping<>( handle, bridge );
		}

		@Override
		public void routingKeyBridge(RoutingKeyBridge bridge) {
			// FIXME ensure the bridge is closed upon build failure and when closing the SearchManagerRepository
			this.routingKeyBridge = bridge;
		}
	}
}