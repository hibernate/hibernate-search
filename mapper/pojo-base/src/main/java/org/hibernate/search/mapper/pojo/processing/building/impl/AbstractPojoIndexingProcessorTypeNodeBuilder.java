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

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundTypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorCastedTypeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorPropertyNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorOriginalTypeNode;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

/**
 * A builder of {@link PojoIndexingProcessorOriginalTypeNode} or {@link PojoIndexingProcessorCastedTypeNode}.
 *
 * @param <T> The type of expected values.
 * @param <U> The processed type: either {@code T}, or another type if casting is necessary before processing.
 */
public abstract class AbstractPojoIndexingProcessorTypeNodeBuilder<T, U> extends AbstractPojoProcessorNodeBuilder
		implements PojoMappingCollectorTypeNode {

	private final Optional<PojoIdentityMappingCollector> identityMappingCollector;
	private final Collection<IndexObjectFieldReference> parentIndexObjectReferences;

	private BoundRoutingKeyBridge<U> boundRoutingKeyBridge;
	private final Collection<BoundTypeBridge<U>> boundBridges = new ArrayList<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoIndexingProcessorPropertyNodeBuilder<U, ?>> propertyNodeBuilders =
			new LinkedHashMap<>();

	public AbstractPojoIndexingProcessorTypeNodeBuilder(
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext,
			Optional<PojoIdentityMappingCollector> identityMappingCollector,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences) {
		super( mappingHelper, bindingContext );
		this.identityMappingCollector = identityMappingCollector;
		this.parentIndexObjectReferences = parentIndexObjectReferences;
	}

	@Override
	public void typeBinder(TypeBinder builder) {
		mappingHelper.getIndexModelBinder().bindType( bindingContext, getModelPath(), builder )
			.ifPresent( boundBridges::add );
	}

	@Override
	public void routingKeyBinder(RoutingKeyBinder builder) {
		if ( identityMappingCollector.isPresent() ) {
			boundRoutingKeyBridge = identityMappingCollector.get().routingKeyBridge( getModelPath(), builder );
		}
	}

	@Override
	public PojoMappingCollectorPropertyNode property(String propertyName) {
		// TODO HSEARCH-3318 also pass an access type ("default" if not mentioned by the user, method/field otherwise) and take it into account to retrieve the right property model/handle
		return propertyNodeBuilders.computeIfAbsent( propertyName, this::createPropertyNodeBuilder );
	}

	private PojoIndexingProcessorPropertyNodeBuilder<U, ?> createPropertyNodeBuilder(String propertyName) {
		return new PojoIndexingProcessorPropertyNodeBuilder<>(
				getModelPath().property( propertyName ),
				mappingHelper, bindingContext, identityMappingCollector
		);
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().get().close(), boundBridges );
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().close(), boundBridges );
			closer.pushAll( PojoIndexingProcessorPropertyNodeBuilder::closeOnFailure, propertyNodeBuilders.values() );
		}
	}

	public Optional<PojoIndexingProcessor<T>> build(PojoIndexingDependencyCollectorValueNode<?, T> valueDependencyCollector) {
		return build( toType( valueDependencyCollector ) );
	}

	public Optional<PojoIndexingProcessor<T>> build(PojoIndexingDependencyCollectorTypeNode<U> dependencyCollector) {
		try {
			return doBuild( dependencyCollector );
		}
		catch (RuntimeException e) {
			failureCollector().add( e );
			return Optional.empty();
		}
	}

	@Override
	abstract BoundPojoModelPathTypeNode<U> getModelPath();

	protected abstract PojoIndexingDependencyCollectorTypeNode<U> toType(
			PojoIndexingDependencyCollectorValueNode<?, T> valueDependencyCollector);

	protected abstract PojoIndexingProcessor<T> doBuild(
			Collection<IndexObjectFieldReference> parentIndexObjectReferences,
			Collection<BeanHolder<? extends TypeBridge>> immutableBridgeHolders,
			PojoIndexingProcessor<? super U> nested);

	private Optional<PojoIndexingProcessor<T>> doBuild(PojoIndexingDependencyCollectorTypeNode<U> dependencyCollector) {
		if ( boundRoutingKeyBridge != null ) {
			boundRoutingKeyBridge.contributeDependencies( dependencyCollector );
		}

		Collection<PojoIndexingProcessorPropertyNode<? super U, ?>> immutablePropertyNodes =
				propertyNodeBuilders.isEmpty() ? Collections.emptyList()
						: new ArrayList<>( propertyNodeBuilders.size() );
		try {
			Collection<BeanHolder<? extends TypeBridge>> immutableBridgeHolders = boundBridges.isEmpty()
					? Collections.emptyList() : new ArrayList<>();
			for ( BoundTypeBridge<U> boundBridge : boundBridges ) {
				immutableBridgeHolders.add( boundBridge.getBridgeHolder() );
				boundBridge.contributeDependencies( dependencyCollector );
			}
			propertyNodeBuilders.values().stream()
					.map( builder -> builder.build( dependencyCollector ) )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.forEach( immutablePropertyNodes::add );

			if ( parentIndexObjectReferences.isEmpty() && immutableBridgeHolders.isEmpty() && immutablePropertyNodes
					.isEmpty() ) {
				/*
				 * If this node doesn't create any object in the document, and it doesn't have any bridge,
				 * nor any property node, then it is useless and we don't need to build it.
				 */
				return Optional.empty();
			}
			else {
				return Optional.of( doBuild(
						parentIndexObjectReferences, immutableBridgeHolders,
						createNested( immutablePropertyNodes )
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
