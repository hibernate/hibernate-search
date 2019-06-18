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
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.BoundPropertyBridge;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorPropertyNode;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

/**
 * A builder of {@link PojoIndexingProcessorPropertyNode}.
 *
 * @param <T> The property holder type
 * @param <P> The property type
 */
class PojoIndexingProcessorPropertyNodeBuilder<T, P> extends AbstractPojoProcessorNodeBuilder
		implements PojoMappingCollectorPropertyNode {

	private final BoundPojoModelPathPropertyNode<T, P> modelPath;

	private final Optional<PojoIdentityMappingCollector> identityMappingCollector;

	private final Collection<BoundPropertyBridge<P>> boundPropertyBridges = new ArrayList<>();
	private final PojoIndexingProcessorValueNodeBuilderDelegate<P, P> valueWithoutExtractorBuilderDelegate;
	// Use a LinkedHashMap for deterministic iteration
	private final Map<ContainerExtractorPath, PojoIndexingProcessorContainerElementNodeBuilder<P, ? super P, ?>>
			containerElementNodeBuilders = new LinkedHashMap<>();

	PojoIndexingProcessorPropertyNodeBuilder(
			BoundPojoModelPathPropertyNode<T, P> modelPath,
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext,
			Optional<PojoIdentityMappingCollector> identityMappingCollector) {
		super( mappingHelper, bindingContext );

		this.modelPath = modelPath;

		this.identityMappingCollector = identityMappingCollector;

		this.valueWithoutExtractorBuilderDelegate = new PojoIndexingProcessorValueNodeBuilderDelegate<>(
				modelPath.valueWithoutExtractors(),
				mappingHelper, bindingContext,
				false
		);
	}

	@Override
	public void bridge(BridgeBuilder<? extends PropertyBridge> builder) {
		mappingHelper.getIndexModelBinder().addPropertyBridge( bindingContext, modelPath, builder )
				.ifPresent( boundPropertyBridges::add );
	}

	@Override
	public void identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		if ( identityMappingCollector.isPresent() ) {
			identityMappingCollector.get().identifierBridge( modelPath, builder );
		}
	}

	@Override
	public PojoMappingCollectorValueNode value(ContainerExtractorPath extractorPath) {
		if ( !extractorPath.isEmpty() ) {
			PojoIndexingProcessorContainerElementNodeBuilder<P, ? super P, ?> containerElementNodeBuilder =
					containerElementNodeBuilders.get( extractorPath );
			if ( containerElementNodeBuilder == null && !containerElementNodeBuilders.containsKey( extractorPath ) ) {
				BoundContainerExtractorPath<P, ?> boundExtractorPath =
						mappingHelper.getIndexModelBinder().bindExtractorPath(
								modelPath.getPropertyModel().getTypeModel(),
								extractorPath
						);
				ContainerExtractorPath explicitExtractorPath = boundExtractorPath.getExtractorPath();
				if ( !explicitExtractorPath.isEmpty() ) {
					// Check whether the path was already encountered as an explicit path
					containerElementNodeBuilder = containerElementNodeBuilders.get( explicitExtractorPath );
					if ( containerElementNodeBuilder == null ) {
						containerElementNodeBuilder = createContainerElementNodeBuilder( boundExtractorPath );
					}
				}
				containerElementNodeBuilders.put( explicitExtractorPath, containerElementNodeBuilder );
				containerElementNodeBuilders.put( extractorPath, containerElementNodeBuilder );
			}
			if ( containerElementNodeBuilder != null ) {
				return containerElementNodeBuilder.value();
			}
		}
		return valueWithoutExtractorBuilderDelegate;
	}

	/*
	 * This generic method is necessary to make it clear to the compiler
	 * that the extracted type and extractor have compatible generic arguments.
	 */
	private <V> PojoIndexingProcessorContainerElementNodeBuilder<P, ? super P, V> createContainerElementNodeBuilder(
			BoundContainerExtractorPath<P, V> boundExtractorPath) {
		BoundPojoModelPathValueNode<T, P, V> containerElementPath = modelPath.value( boundExtractorPath );
		ContainerExtractorHolder<P, V> extractorHolder =
				mappingHelper.getIndexModelBinder().createExtractors( boundExtractorPath );
		return new PojoIndexingProcessorContainerElementNodeBuilder<>(
				containerElementPath, extractorHolder,
				mappingHelper, bindingContext
		);
	}

	@Override
	BoundPojoModelPath getModelPath() {
		return modelPath;
	}

	@Override
	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().get().close(), boundPropertyBridges );
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().close(), boundPropertyBridges );
			closer.push(
					PojoIndexingProcessorValueNodeBuilderDelegate::closeOnFailure,
					valueWithoutExtractorBuilderDelegate
			);
			closer.pushAll(
					PojoIndexingProcessorContainerElementNodeBuilder::closeOnFailure,
					containerElementNodeBuilders.values()
			);
		}
	}

	Optional<PojoIndexingProcessorPropertyNode<T, P>> build(
			PojoIndexingDependencyCollectorTypeNode<T> parentDependencyCollector) {
		try {
			return doBuild( parentDependencyCollector );
		}
		catch (RuntimeException e) {
			getFailureCollector().add( e );
			return Optional.empty();
		}
	}

	private Optional<PojoIndexingProcessorPropertyNode<T, P>> doBuild(
			PojoIndexingDependencyCollectorTypeNode<T> parentDependencyCollector) {
		@SuppressWarnings("unchecked") // We know from the property model that this property has type P
		PojoIndexingDependencyCollectorPropertyNode<T, P> propertyDependencyCollector =
				(PojoIndexingDependencyCollectorPropertyNode<T, P>)
						parentDependencyCollector.property( modelPath.getPropertyModel().getName() );

		Collection<PojoIndexingProcessor<? super P>> immutableNestedNodes = Collections.emptyList();
		try {
			Collection<BeanHolder<? extends PropertyBridge>> immutableBridgeHolders = boundPropertyBridges.isEmpty()
					? Collections.emptyList() : new ArrayList<>();
			for ( BoundPropertyBridge<P> boundBridge : boundPropertyBridges ) {
				immutableBridgeHolders.add( boundBridge.getBridgeHolder() );
				boundBridge.contributeDependencies( propertyDependencyCollector );
			}
			Collection<PojoIndexingProcessor<? super P>> valueWithoutExtractorNodes =
					valueWithoutExtractorBuilderDelegate.build( propertyDependencyCollector );
			if ( !valueWithoutExtractorNodes.isEmpty() || !containerElementNodeBuilders.isEmpty() ) {
				immutableNestedNodes = new ArrayList<>(
						valueWithoutExtractorNodes.size() + containerElementNodeBuilders.size()
				);
			}
			if ( !valueWithoutExtractorNodes.isEmpty() ) {
				immutableNestedNodes.addAll( valueWithoutExtractorNodes );
			}
			containerElementNodeBuilders.values().stream()
					.distinct() // Necessary because the default extractor path has two possible keys with the same value
					.filter( Objects::nonNull )
					.map( builder -> builder.build( propertyDependencyCollector ) )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.forEach( immutableNestedNodes::add );

			if ( immutableBridgeHolders.isEmpty() && immutableNestedNodes.isEmpty() ) {
				/*
				 * If this node doesn't have any bridge, nor any nested node,
				 * it is useless and we don't need to build it.
				 */
				return Optional.empty();
			}
			else {
				return Optional.of( new PojoIndexingProcessorPropertyNode<>(
						modelPath.getPropertyHandle(), immutableBridgeHolders, immutableNestedNodes
				) );
			}
		}
		catch (RuntimeException e) {
			// Close the nested processors created so far before aborting
			new SuppressingCloser( e )
					.pushAll( PojoIndexingProcessor::close, immutableNestedNodes );
			throw e;
		}
	}
}
