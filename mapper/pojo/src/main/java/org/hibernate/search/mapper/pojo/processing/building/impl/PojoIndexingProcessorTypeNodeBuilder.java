/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.mapping.building.impl.BoundTypeBridge;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorPropertyNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorTypeNode;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.SuppressingCloser;

/**
 * A builder of {@link PojoIndexingProcessorTypeNode}.
 *
 * @param <T> The processed type
 */
public class PojoIndexingProcessorTypeNodeBuilder<T> extends AbstractPojoProcessorNodeBuilder<T>
		implements PojoMappingCollectorTypeNode {

	private final BoundPojoModelPathTypeNode<T> modelPath;

	private final Optional<PojoIdentityMappingCollector> identityMappingCollector;

	private BoundRoutingKeyBridge<T> boundRoutingKeyBridge;
	private final Collection<BoundTypeBridge<T>> boundBridges = new ArrayList<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<PropertyHandle, PojoIndexingProcessorPropertyNodeBuilder<T, ?>> propertyNodeBuilders =
			new LinkedHashMap<>();

	public PojoIndexingProcessorTypeNodeBuilder(
			BoundPojoModelPathTypeNode<T> modelPath,
			PojoMappingHelper mappingHelper, IndexModelBindingContext bindingContext,
			Optional<PojoIdentityMappingCollector> identityMappingCollector) {
		super( mappingHelper, bindingContext );

		this.modelPath = modelPath;

		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	public void bridge(BridgeBuilder<? extends TypeBridge> builder) {
		mappingHelper.getIndexModelBinder().addTypeBridge( bindingContext, modelPath, builder )
				.ifPresent( boundBridges::add );
	}

	@Override
	public void routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder) {
		if ( identityMappingCollector.isPresent() ) {
			boundRoutingKeyBridge = mappingHelper.getIndexModelBinder()
					.addRoutingKeyBridge( bindingContext, modelPath, builder );
			identityMappingCollector.get().routingKeyBridge( boundRoutingKeyBridge.getBridge() );
		}
	}

	@Override
	public PojoMappingCollectorPropertyNode property(PropertyHandle propertyHandle) {
		return propertyNodeBuilders.computeIfAbsent( propertyHandle, this::createPropertyNodeBuilder );
	}

	private PojoIndexingProcessorPropertyNodeBuilder<T, ?> createPropertyNodeBuilder(PropertyHandle propertyHandle) {
		return new PojoIndexingProcessorPropertyNodeBuilder<>(
				modelPath.property( propertyHandle ),
				mappingHelper, bindingContext, identityMappingCollector
		);
	}

	@Override
	BoundPojoModelPathTypeNode<T> getModelPath() {
		return modelPath;
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( boundBridge -> boundBridge.getBridge().close(), boundBridges );
			closer.pushAll( PojoIndexingProcessorPropertyNodeBuilder::closeOnFailure, propertyNodeBuilders.values() );
		}
	}

	public Optional<PojoIndexingProcessor<T>> build(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		Collection<IndexObjectFieldAccessor> parentIndexObjectAccessors = bindingContext.getParentIndexObjectAccessors();

		if ( boundRoutingKeyBridge != null ) {
			boundRoutingKeyBridge.getPojoModelRootElement().contributeDependencies( dependencyCollector );
		}

		Collection<PojoIndexingProcessorPropertyNode<? super T, ?>> immutablePropertyNodes =
				propertyNodeBuilders.isEmpty() ? Collections.emptyList()
						: new ArrayList<>( propertyNodeBuilders.size() );
		try {
			Collection<TypeBridge> immutableBridges = boundBridges.isEmpty()
					? Collections.emptyList() : new ArrayList<>();
			for ( BoundTypeBridge<T> boundBridge : boundBridges ) {
				immutableBridges.add( boundBridge.getBridge() );
				boundBridge.getPojoModelRootElement().contributeDependencies( dependencyCollector );
			}
			propertyNodeBuilders.values().stream()
					.map( builder -> builder.build( dependencyCollector ) )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.forEach( immutablePropertyNodes::add );

			if ( parentIndexObjectAccessors.isEmpty() && immutableBridges.isEmpty() && immutablePropertyNodes
					.isEmpty() ) {
				/*
				 * If this node doesn't create any object in the document, and it doesn't have any bridge,
				 * nor any property node, then it is useless and we don't need to build it.
				 */
				return Optional.empty();
			}
			else {
				return Optional.of( new PojoIndexingProcessorTypeNode<>(
						parentIndexObjectAccessors, immutableBridges, immutablePropertyNodes
				) );
			}
		}
		catch (RuntimeException e) {
			// Close the nested processors created so far before aborting
			new SuppressingCloser( e )
					.pushAll( PojoIndexingProcessor::close, immutablePropertyNodes );
			throw e;
		}
	}

}
