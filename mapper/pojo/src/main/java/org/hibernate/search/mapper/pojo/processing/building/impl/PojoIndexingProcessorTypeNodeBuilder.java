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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorPropertyNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorTypeNode;

public class PojoIndexingProcessorTypeNodeBuilder<T> extends AbstractPojoProcessorNodeBuilder<T>
		implements PojoMappingCollectorTypeNode {

	private final BoundPojoModelPathTypeNode<T> modelPath;
	private final PojoModelTypeRootElement pojoModelRootElement;

	private final PojoIdentityMappingCollector identityMappingCollector;

	private final Collection<TypeBridge> bridges = new ArrayList<>();
	private final Map<PropertyHandle, PojoIndexingProcessorPropertyNodeBuilder<? super T, ?>> propertyNodeBuilders =
			new HashMap<>();

	public PojoIndexingProcessorTypeNodeBuilder(
			BoundPojoModelPathTypeNode<T> modelPath,
			PojoMappingHelper mappingHelper, IndexModelBindingContext bindingContext,
			PojoIdentityMappingCollector identityMappingCollector) {
		super( mappingHelper, bindingContext );

		this.modelPath = modelPath;

		// FIXME do something more with the pojoModelRootElement, to be able to use it in containedIn processing in particular
		this.pojoModelRootElement = new PojoModelTypeRootElement(
				modelPath.getTypeModel(), mappingHelper.getAugmentedTypeModelProvider()
		);

		this.identityMappingCollector = identityMappingCollector;
	}

	@Override
	public void bridge(BridgeBuilder<? extends TypeBridge> builder) {
		mappingHelper.getIndexModelBinder().addTypeBridge( bindingContext, pojoModelRootElement, builder )
				.ifPresent( bridges::add );
	}

	@Override
	public void routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder) {
		RoutingKeyBridge bridge = mappingHelper.getIndexModelBinder()
				.addRoutingKeyBridge( bindingContext, pojoModelRootElement, builder );
		identityMappingCollector.routingKeyBridge( bridge );
	}

	@Override
	public PojoMappingCollectorPropertyNode property(PropertyHandle propertyHandle) {
		return propertyNodeBuilders.computeIfAbsent( propertyHandle, this::createPropertyNodeBuilder );
	}

	private PojoIndexingProcessorPropertyNodeBuilder<? super T, ?> createPropertyNodeBuilder(PropertyHandle propertyHandle) {
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
	public Optional<PojoIndexingProcessor<T>> build() {
		Collection<IndexObjectFieldAccessor> parentIndexObjectAccessors = bindingContext.getParentIndexObjectAccessors();
		Collection<TypeBridge> immutableBridges = bridges.isEmpty() ? Collections.emptyList() : new ArrayList<>( bridges );
		Collection<PojoIndexingProcessorPropertyNode<? super T, ?>> immutablePropertyNodes =
				propertyNodeBuilders.isEmpty() ? Collections.emptyList()
						: new ArrayList<>( propertyNodeBuilders.size() );
		propertyNodeBuilders.values().stream()
				.map( PojoIndexingProcessorPropertyNodeBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutablePropertyNodes::add );

		if ( parentIndexObjectAccessors.isEmpty() && immutableBridges.isEmpty() && immutablePropertyNodes.isEmpty() ) {
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

}
