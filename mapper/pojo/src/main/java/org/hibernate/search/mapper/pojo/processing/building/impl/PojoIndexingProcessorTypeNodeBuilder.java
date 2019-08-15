/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundTypeBridge;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorPropertyNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorTypeNode;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A builder of {@link PojoIndexingProcessorTypeNode}.
 *
 * @param <T> The processed type
 */
public class PojoIndexingProcessorTypeNodeBuilder<T> extends AbstractPojoProcessorNodeBuilder
		implements PojoMappingCollectorTypeNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BoundPojoModelPathTypeNode<T> modelPath;

	private final Optional<PojoIdentityMappingCollector> identityMappingCollector;
	private final Collection<IndexObjectFieldReference> parentIndexObjectReferences;

	private BoundRoutingKeyBridge<T> boundRoutingKeyBridge;
	private final Collection<BoundTypeBridge<T>> boundBridges = new ArrayList<>();
	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, PojoIndexingProcessorPropertyNodeBuilder<T, ?>> propertyNodeBuilders =
			new LinkedHashMap<>();

	public PojoIndexingProcessorTypeNodeBuilder(
			BoundPojoModelPathTypeNode<T> modelPath,
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext,
			Optional<PojoIdentityMappingCollector> identityMappingCollector,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences) {
		super( mappingHelper, bindingContext );

		this.modelPath = modelPath;

		this.identityMappingCollector = identityMappingCollector;
		this.parentIndexObjectReferences = parentIndexObjectReferences;
	}

	@Override
	public void typeBinder(TypeBinder<?> builder) {
		mappingHelper.getIndexModelBinder().bindType( bindingContext, modelPath, builder )
			.ifPresent( boundBridges::add );
	}

	@Override
	public void routingKeyBinder(RoutingKeyBinder<?> builder) {
		if ( identityMappingCollector.isPresent() ) {
			boundRoutingKeyBridge = identityMappingCollector.get().routingKeyBridge( modelPath, builder );
		}
	}

	@Override
	public PojoMappingCollectorPropertyNode property(String propertyName) {
		// TODO HSEARCH-3318 also pass an access type ("default" if not mentioned by the user, method/field otherwise) and take it into account to retrieve the right property model/handle
		return propertyNodeBuilders.computeIfAbsent( propertyName, this::createPropertyNodeBuilder );
	}

	private PojoIndexingProcessorPropertyNodeBuilder<T, ?> createPropertyNodeBuilder(String propertyName) {
		return new PojoIndexingProcessorPropertyNodeBuilder<>(
				modelPath.property( propertyName ),
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
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().get().close(), boundBridges );
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().close(), boundBridges );
			closer.pushAll( PojoIndexingProcessorPropertyNodeBuilder::closeOnFailure, propertyNodeBuilders.values() );
		}
	}

	public Optional<PojoIndexingProcessor<T>> build(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		try {
			return doBuild( dependencyCollector );
		}
		catch (RuntimeException e) {
			getFailureCollector().add( e );
			return Optional.empty();
		}
	}

	private Optional<PojoIndexingProcessor<T>> doBuild(PojoIndexingDependencyCollectorTypeNode<T> dependencyCollector) {
		if ( boundRoutingKeyBridge != null ) {
			boundRoutingKeyBridge.contributeDependencies( dependencyCollector );
		}

		Collection<PojoIndexingProcessorPropertyNode<? super T, ?>> immutablePropertyNodes =
				propertyNodeBuilders.isEmpty() ? Collections.emptyList()
						: new ArrayList<>( propertyNodeBuilders.size() );
		try {
			Collection<BeanHolder<? extends TypeBridge>> immutableBridgeHolders = boundBridges.isEmpty()
					? Collections.emptyList() : new ArrayList<>();
			for ( BoundTypeBridge<T> boundBridge : boundBridges ) {
				immutableBridgeHolders.add( boundBridge.getBridgeHolder() );
				boundBridge.contributeDependencies( dependencyCollector );
			}
			propertyNodeBuilders.values().stream()
					.map( builder -> builder.build( dependencyCollector ) )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.forEach( immutablePropertyNodes::add );

			if ( immutableBridgeHolders.isEmpty() && immutablePropertyNodes.isEmpty() ) {
				if ( parentIndexObjectReferences.isEmpty() ) {
					/*
					 * If this node doesn't create any object in the document, and it doesn't have any bridge,
					 * nor any property node, then it is useless and we don't need to build it.
					 */
					return Optional.empty();
				}

				throw log.invalidIndexedEmbedded( modelPath.getTypeModel() );
			}
			else {
				return Optional.of( new PojoIndexingProcessorTypeNode<>(
						parentIndexObjectReferences, immutableBridgeHolders, immutablePropertyNodes
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
