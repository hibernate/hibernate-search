/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.AbstractPojoIndexingDependencyCollectorDirectValueNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundTypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.identity.impl.PojoIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorCastedTypeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorOriginalTypeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorTypeBridgeNode;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

/**
 * A builder of {@link PojoIndexingProcessorOriginalTypeNode} or {@link PojoIndexingProcessorCastedTypeNode}.
 *
 * @param <T> The type of expected values.
 * @param <U> The processed type: either {@code T}, or another type if casting is necessary before processing.
 */
public abstract class AbstractPojoIndexingProcessorTypeNodeBuilder<T, U> extends AbstractPojoProcessorNodeBuilder
		implements PojoIndexMappingCollectorTypeNode {

	private final PojoIdentityMappingCollector identityMappingCollector;
	private final Collection<IndexObjectFieldReference> parentIndexObjectReferences;

	private final Collection<BoundTypeBridge<U>> boundBridges = new ArrayList<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoIndexingProcessorPropertyNodeBuilder<U, ?>> propertyNodeBuilders =
			new LinkedHashMap<>();

	public AbstractPojoIndexingProcessorTypeNodeBuilder(
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext,
			PojoIdentityMappingCollector identityMappingCollector,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences) {
		super( mappingHelper, bindingContext );
		this.identityMappingCollector = identityMappingCollector;
		this.parentIndexObjectReferences = parentIndexObjectReferences;
	}

	@Override
	public void typeBinder(TypeBinder builder, Map<String, Object> params) {
		mappingHelper.indexModelBinder().bindType( bindingContext, getModelPath(), builder, params )
				.ifPresent( boundBridges::add );
	}

	@Override
	public PojoIndexMappingCollectorPropertyNode property(String propertyName) {
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
			closer.pushAll( TypeBridge::close, boundBridges, BoundTypeBridge::getBridge );
			closer.pushAll( BeanHolder::close, boundBridges, BoundTypeBridge::getBridgeHolder );
			closer.pushAll( PojoIndexingProcessorPropertyNodeBuilder::closeOnFailure, propertyNodeBuilders.values() );
		}
	}

	public Optional<PojoIndexingProcessor<T>> build(
			AbstractPojoIndexingDependencyCollectorDirectValueNode<?, T> valueDependencyCollector) {
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

	protected PojoTypeAdditionalMetadata typeAdditionalMetadata() {
		return mappingHelper.typeAdditionalMetadataProvider().get( getModelPath().getTypeModel().rawType() );
	}

	protected abstract PojoIndexingDependencyCollectorTypeNode<U> toType(
			AbstractPojoIndexingDependencyCollectorDirectValueNode<?, T> valueDependencyCollector);

	protected abstract PojoIndexingProcessor<T> doBuild(
			Collection<IndexObjectFieldReference> parentIndexObjectReferences,
			PojoIndexingProcessor<? super U> nested);

	private Optional<PojoIndexingProcessor<T>> doBuild(PojoIndexingDependencyCollectorTypeNode<U> dependencyCollector) {
		Collection<PojoIndexingProcessor<? super U>> nestedNodes = new ArrayList<>();
		try {
			for ( BoundTypeBridge<U> boundBridge : boundBridges ) {
				nestedNodes.add( new PojoIndexingProcessorTypeBridgeNode<>( boundBridge.getBridgeHolder() ) );
				boundBridge.contributeDependencies( dependencyCollector );
			}
			propertyNodeBuilders.values().stream()
					.map( builder -> builder.build( dependencyCollector ) )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.forEach( nestedNodes::add );

			if ( parentIndexObjectReferences.isEmpty() && nestedNodes.isEmpty() ) {
				/*
				 * If this node doesn't create any object in the document, and it doesn't have any bridge,
				 * nor any property node, then it is useless and we don't need to build it.
				 */
				return Optional.empty();
			}
			else {
				return Optional.of( doBuild(
						parentIndexObjectReferences,
						createNested( nestedNodes )
				) );
			}
		}
		catch (RuntimeException e) {
			// Close the nested processors created so far before aborting
			new SuppressingCloser( e )
					.pushAll( PojoIndexingProcessor::close, nestedNodes );
			throw e;
		}
	}
}
