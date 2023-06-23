/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorContainerElementNode;
import org.hibernate.search.util.common.impl.Closer;

/**
 * A builder of {@link PojoIndexingProcessorContainerElementNode}.
 *
 * @param <P> The type of the property on which the container value extractor is applied.
 * @param <C> The type of containers accepted by the container value extractor.
 * @param <V> The type of values extracted by the container value extractor.
 */
class PojoIndexingProcessorContainerElementNodeBuilder<P extends C, C, V> extends AbstractPojoProcessorNodeBuilder {

	private final BoundPojoModelPathValueNode<?, P, V> modelPath;
	private final ContainerExtractorHolder<C, V> extractorHolder;

	private final PojoIndexingProcessorValueNodeBuilderDelegate<P, V> valueNodeProcessorCollectionBuilder;

	PojoIndexingProcessorContainerElementNodeBuilder(BoundPojoModelPathValueNode<?, P, V> modelPath,
			ContainerExtractorHolder<C, V> extractorHolder,
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext) {
		super( mappingHelper, bindingContext );
		this.modelPath = modelPath;
		this.extractorHolder = extractorHolder;

		valueNodeProcessorCollectionBuilder = new PojoIndexingProcessorValueNodeBuilderDelegate<>(
				modelPath,
				mappingHelper, bindingContext,
				extractorHolder.multiValued()
		);
	}

	public PojoIndexMappingCollectorValueNode value() {
		return valueNodeProcessorCollectionBuilder;
	}

	@Override
	BoundPojoModelPathValueNode<?, ? extends C, V> getModelPath() {
		return modelPath;
	}

	@Override
	void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( ContainerExtractorHolder::close, extractorHolder );
			closer.pushAll(
					PojoIndexingProcessorValueNodeBuilderDelegate::closeOnFailure,
					valueNodeProcessorCollectionBuilder
			);
		}
	}

	Optional<PojoIndexingProcessorContainerElementNode<C, V>> build(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentDependencyCollector) {
		try {
			return doBuild( parentDependencyCollector );
		}
		catch (RuntimeException e) {
			failureCollector().add( e );
			return Optional.empty();
		}
	}

	private Optional<PojoIndexingProcessorContainerElementNode<C, V>> doBuild(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentDependencyCollector) {
		Collection<PojoIndexingProcessor<? super V>> immutableNestedProcessors =
				valueNodeProcessorCollectionBuilder.build( parentDependencyCollector );

		if ( immutableNestedProcessors.isEmpty() ) {
			/*
			 * If this processor doesn't have any bridge, nor any nested processor,
			 * it is useless and we don't need to build it.
			 * Release the other resources (the container value extractors) and return.
			 */
			extractorHolder.close();
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoIndexingProcessorContainerElementNode<>(
					extractorHolder, createNested( immutableNestedProcessors )
			) );
		}
	}
}
