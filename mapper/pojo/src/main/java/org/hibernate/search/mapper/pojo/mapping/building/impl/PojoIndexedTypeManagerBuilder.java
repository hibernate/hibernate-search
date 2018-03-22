/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.impl.IdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.impl.PropertyIdentifierMapping;
import org.hibernate.search.mapper.pojo.mapping.impl.RoutingKeyBridgeRoutingKeyProvider;
import org.hibernate.search.mapper.pojo.mapping.impl.RoutingKeyProvider;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.processing.building.impl.PojoIndexingProcessorTypeNodeBuilder;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class PojoIndexedTypeManagerBuilder<E, D extends DocumentElement> {
	private static Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoRawTypeModel<E> typeModel;
	private final IndexManagerBuildingState<D> indexManagerBuildingState;

	private final PojoIdentityMappingCollectorImpl identityMappingCollector;
	private final PojoIndexingProcessorTypeNodeBuilder<E> processorBuilder;

	PojoIndexedTypeManagerBuilder(PojoRawTypeModel<E> typeModel,
			PojoMappingHelper mappingHelper,
			IndexManagerBuildingState<D> indexManagerBuildingState,
			IdentifierMapping<?, E> defaultIdentifierMapping) {
		this.typeModel = typeModel;
		this.indexManagerBuildingState = indexManagerBuildingState;
		this.identityMappingCollector = new PojoIdentityMappingCollectorImpl( defaultIdentifierMapping );
		IndexModelBindingContext bindingContext = indexManagerBuildingState.getRootBindingContext();
		this.processorBuilder = new PojoIndexingProcessorTypeNodeBuilder<>(
				BoundPojoModelPath.root( typeModel ),
				mappingHelper, bindingContext, identityMappingCollector
		);
	}

	public PojoMappingCollectorTypeNode asCollector() {
		return processorBuilder;
	}

	public void addTo(PojoIndexedTypeManagerContainer.Builder builder) {
		IdentifierMapping<?, E> identifierMapping = identityMappingCollector.identifierMapping;
		if ( identifierMapping == null ) {
			throw new SearchException( "Missing identifier mapping for indexed type '" + typeModel + "'" );
		}
		RoutingKeyBridge routingKeyBridge = identityMappingCollector.routingKeyBridge;
		RoutingKeyProvider<E> routingKeyProvider;
		if ( routingKeyBridge == null ) {
			routingKeyProvider = RoutingKeyProvider.alwaysNull();
		}
		else {
			routingKeyProvider = new RoutingKeyBridgeRoutingKeyProvider<>( routingKeyBridge );
		}
		PojoIndexedTypeManager<?, E, D> typeManager = new PojoIndexedTypeManager<>(
				typeModel.getJavaClass(), typeModel.getCaster(),
				identifierMapping, routingKeyProvider,
				processorBuilder.build().orElseGet( PojoIndexingProcessor::noOp ),
				indexManagerBuildingState.build()
		);
		log.createdPojoIndexedTypeManager( typeManager );
		builder.add( indexManagerBuildingState.getIndexName(), typeModel, typeManager );
	}

	private class PojoIdentityMappingCollectorImpl implements PojoIdentityMappingCollector {
		private IdentifierMapping<?, E> identifierMapping;
		private RoutingKeyBridge routingKeyBridge;

		PojoIdentityMappingCollectorImpl(IdentifierMapping<?, E> identifierMapping) {
			this.identifierMapping = identifierMapping;
		}

		@Override
		public <T> void identifierBridge(PojoTypeModel<T> propertyTypeModel, PropertyHandle handle, IdentifierBridge<T> bridge) {
			// FIXME ensure the bridge is closed upon build failure and when closing the SearchManagerRepository
			this.identifierMapping = new PropertyIdentifierMapping<>( propertyTypeModel.getRawType().getCaster(), handle, bridge );
		}

		@Override
		public void routingKeyBridge(RoutingKeyBridge bridge) {
			// FIXME ensure the bridge is closed upon build failure and when closing the SearchManagerRepository
			this.routingKeyBridge = bridge;
		}
	}
}