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
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedDefinition;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEmbeddedBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundValueBridge;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorValueBridgeNode;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A delegate to be used by {@link PojoIndexingProcessorPropertyNodeBuilder}
 * and {@link PojoIndexingProcessorContainerElementNodeBuilder}.
 *
 * @param <P> The type of the property from which values are retrieved (either directly or using an extractor).
 * @param <V> The type of values extracted by the container value extractor.
 */
class PojoIndexingProcessorValueNodeBuilderDelegate<P, V> extends AbstractPojoProcessorNodeBuilder
		implements PojoMappingCollectorValueNode {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BoundPojoModelPathValueNode<?, P, V> modelPath;

	private final Collection<BoundValueBridge<V, ?>> boundBridges = new ArrayList<>();

	private final Collection<AbstractPojoIndexingProcessorTypeNodeBuilder<V, ?>> typeNodeBuilders = new ArrayList<>();

	private final boolean multiValuedFromContainerExtractor;

	PojoIndexingProcessorValueNodeBuilderDelegate(
			BoundPojoModelPathValueNode<?, P, V> modelPath,
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext,
			boolean multiValuedFromContainerExtractor) {
		super( mappingHelper, bindingContext );
		this.modelPath = modelPath;
		this.multiValuedFromContainerExtractor = multiValuedFromContainerExtractor;
	}

	@Override
	public void valueBinder(ValueBinder binder, String relativeFieldName,
			FieldModelContributor fieldModelContributor) {
		String defaultedRelativeFieldName = relativeFieldName;
		if ( defaultedRelativeFieldName == null ) {
			defaultedRelativeFieldName = modelPath.getParent().getPropertyModel().name();
		}

		mappingHelper.getIndexModelBinder().bindValue(
				bindingContext, modelPath, multiValuedFromContainerExtractor,
				binder, defaultedRelativeFieldName,
				fieldModelContributor
		)
				.ifPresent( boundBridges::add );
	}

	@Override
	public void indexedEmbedded(PojoRawTypeModel<?> definingTypeModel, String relativePrefix,
			ObjectStructure structure,
			Integer maxDepth, Set<String> includePaths,
			Class<?> targetType) {
		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = modelPath.getParent().getPropertyModel().name() + ".";
		}

		IndexedEmbeddedDefinition definition = new IndexedEmbeddedDefinition(
				definingTypeModel, defaultedRelativePrefix, structure,
				maxDepth, includePaths
		);

		Optional<IndexedEmbeddedBindingContext> nestedBindingContextOptional = bindingContext.addIndexedEmbeddedIfIncluded(
				definition, multiValuedFromContainerExtractor
		);
		nestedBindingContextOptional.ifPresent( nestedBindingContext -> {
			AbstractPojoIndexingProcessorTypeNodeBuilder<V, ?> nestedProcessorBuilder;
			if ( targetType == null ) {
				nestedProcessorBuilder = new PojoIndexingProcessorOriginalTypeNodeBuilder<>(
						modelPath.type(), mappingHelper, nestedBindingContext,
						// Do NOT propagate the identity mapping collector to IndexedEmbeddeds
						Optional.empty(),
						nestedBindingContext.parentIndexObjectReferences()
				);
			}
			else {
				PojoRawTypeModel<?> castedType = mappingHelper.getIntrospector().typeModel( targetType );
				nestedProcessorBuilder = new PojoIndexingProcessorCastedTypeNodeBuilder<>(
						modelPath.castedType( castedType ), mappingHelper, nestedBindingContext,
						// Do NOT propagate the identity mapping collector to IndexedEmbeddeds
						Optional.empty(),
						nestedBindingContext.parentIndexObjectReferences()
				);
			}
			typeNodeBuilders.add( nestedProcessorBuilder );

			PojoTypeModel<?> targetTypeModel = nestedProcessorBuilder.getModelPath().getTypeModel();

			Set<PojoTypeMetadataContributor> contributors = mappingHelper.getContributorProvider()
					.get( targetTypeModel.rawType() );
			if ( contributors.isEmpty() ) {
				throw log.invalidIndexedEmbedded( targetTypeModel );
			}
			contributors.forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
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
			closer.pushAll( AbstractPojoIndexingProcessorTypeNodeBuilder::closeOnFailure, typeNodeBuilders );
		}
	}

	Collection<PojoIndexingProcessor<? super V>> build(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentDependencyCollector) {
		try {
			return doBuild( parentDependencyCollector );
		}
		catch (RuntimeException e) {
			failureCollector().add( e );
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
					.map( builder -> builder.build( valueDependencyCollector ) )
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
