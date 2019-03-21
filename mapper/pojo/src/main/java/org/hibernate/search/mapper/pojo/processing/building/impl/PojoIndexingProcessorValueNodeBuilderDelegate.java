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
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.NonRootIndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.BoundValueBridge;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorValueBridgeNode;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;

/**
 * A delegate to be used by {@link PojoIndexingProcessorPropertyNodeBuilder}
 * and {@link PojoIndexingProcessorContainerElementNodeBuilder}.
 *
 * @param <P> The type of the property from which values are retrieved (either directly or using an extractor).
 * @param <V> The type of values extracted by the container value extractor.
 */
class PojoIndexingProcessorValueNodeBuilderDelegate<P, V> extends AbstractPojoProcessorNodeBuilder
		implements PojoMappingCollectorValueNode {

	private final BoundPojoModelPathValueNode<?, P, V> modelPath;

	private final Collection<BoundValueBridge<V, ?>> boundBridges = new ArrayList<>();

	private final Collection<PojoIndexingProcessorTypeNodeBuilder<V>> typeNodeBuilders = new ArrayList<>();

	PojoIndexingProcessorValueNodeBuilderDelegate(
			BoundPojoModelPathValueNode<?, P, V> modelPath,
			PojoMappingHelper mappingHelper, IndexModelBindingContext bindingContext) {
		super( mappingHelper, bindingContext );
		this.modelPath = modelPath;
	}

	@Override
	public void valueBridge(BridgeBuilder<? extends ValueBridge<?, ?>> builder, String relativeFieldName,
			FieldModelContributor fieldModelContributor) {
		String defaultedRelativeFieldName = relativeFieldName;
		if ( defaultedRelativeFieldName == null ) {
			defaultedRelativeFieldName = modelPath.getParent().getPropertyHandle().getName();
		}

		mappingHelper.getIndexModelBinder().addValueBridge(
				bindingContext, modelPath, builder, defaultedRelativeFieldName,
				fieldModelContributor
		)
				.ifPresent( boundBridges::add );
	}

	@Override
	public void indexedEmbedded(String relativePrefix, ObjectFieldStorage storage,
			Integer maxDepth, Set<String> includePaths) {
		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = modelPath.getParent().getPropertyHandle().getName() + ".";
		}

		BoundPojoModelPathTypeNode<?> holderTypePath = modelPath.getParent().getParent();

		Optional<NonRootIndexModelBindingContext> nestedBindingContextOptional = bindingContext.addIndexedEmbeddedIfIncluded(
				holderTypePath.getTypeModel().getRawType(),
				defaultedRelativePrefix, storage, maxDepth, includePaths
		);
		nestedBindingContextOptional.ifPresent( nestedBindingContext -> {
			BoundPojoModelPathTypeNode<V> embeddedTypeModelPath = modelPath.type();
			PojoIndexingProcessorTypeNodeBuilder<V> nestedProcessorBuilder = new PojoIndexingProcessorTypeNodeBuilder<>(
					embeddedTypeModelPath, mappingHelper, nestedBindingContext,
					// Do NOT propagate the identity mapping collector to IndexedEmbeddeds
					Optional.empty(),
					nestedBindingContext.getParentIndexObjectReferences()
			);
			typeNodeBuilders.add( nestedProcessorBuilder );
			mappingHelper.getContributorProvider().forEach(
					embeddedTypeModelPath.getTypeModel().getRawType(),
					c -> c.contributeMapping( nestedProcessorBuilder )
			);
		} );
	}

	@Override
	BoundPojoModelPathValueNode<?, P, V> getModelPath() {
		return modelPath;
	}

	@Override
	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().get().close(), boundBridges );
			closer.pushAll( boundBridge -> boundBridge.getBridgeHolder().close(), boundBridges );
			closer.pushAll( PojoIndexingProcessorTypeNodeBuilder::closeOnFailure, typeNodeBuilders );
		}
	}

	Collection<PojoIndexingProcessor<? super V>> build(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentDependencyCollector) {
		try {
			return doBuild( parentDependencyCollector );
		}
		catch (RuntimeException e) {
			getFailureCollector().add( e );
			return Collections.emptyList();
		}
	}

	private Collection<PojoIndexingProcessor<? super V>> doBuild(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentDependencyCollector) {
		PojoIndexingDependencyCollectorValueNode<P, V> valueDependencyCollector =
				parentDependencyCollector.value( modelPath.getBoundExtractorPath() );

		Collection<PojoIndexingProcessor<? super V>> immutableNestedNodes =
				boundBridges.isEmpty() && typeNodeBuilders.isEmpty()
						? Collections.emptyList()
						: new ArrayList<>( boundBridges.size() + typeNodeBuilders.size() );
		try {
			for ( BoundValueBridge<? super V, ?> boundBridge : boundBridges ) {
				immutableNestedNodes.add( createValueBridgeNode( boundBridge ) );
			}
			typeNodeBuilders.stream()
					.map( builder -> builder.build( valueDependencyCollector.type() ) )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.forEach( immutableNestedNodes::add );

			if ( !immutableNestedNodes.isEmpty() ) {
				valueDependencyCollector.collectDependency();
			}

			return immutableNestedNodes;
		}
		catch (RuntimeException e) {
			// Close the nested processors created so far before aborting
			new SuppressingCloser( e ).pushAll( PojoIndexingProcessor::close, immutableNestedNodes );
			throw e;
		}
	}

	private static <V, F> PojoIndexingProcessor<V> createValueBridgeNode(BoundValueBridge<V, F> boundBridge) {
		return new PojoIndexingProcessorValueBridgeNode<>(
				boundBridge.getBridgeHolder(), boundBridge.getIndexFieldReference()
		);
	}
}
