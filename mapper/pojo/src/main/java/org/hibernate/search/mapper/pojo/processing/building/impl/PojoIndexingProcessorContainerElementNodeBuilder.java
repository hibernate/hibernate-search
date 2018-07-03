/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.dirtiness.building.impl.PojoIndexingDependencyCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorContainerElementNode;

/**
 * A builder of {@link PojoIndexingProcessorContainerElementNode}.
 *
 * @param <P> The type of the property on which the container value extractor is applied.
 * @param <C> The type of containers accepted by the container value extractor.
 * @param <V> The type of values extracted by the container value extractor.
 */
class PojoIndexingProcessorContainerElementNodeBuilder<P extends C, C, V> extends AbstractPojoProcessorNodeBuilder {

	private final BoundPojoModelPathValueNode<?, P, V> modelPath;
	private final ContainerValueExtractor<C, V> extractor;

	private final PojoIndexingProcessorValueNodeBuilderDelegate<P, V> valueNodeProcessorCollectionBuilder;

	PojoIndexingProcessorContainerElementNodeBuilder(BoundPojoModelPathValueNode<?, P, V> modelPath,
			ContainerValueExtractor<C, V> extractor,
			PojoMappingHelper mappingHelper, IndexModelBindingContext bindingContext) {
		super( mappingHelper, bindingContext );
		this.modelPath = modelPath;
		this.extractor = extractor;

		valueNodeProcessorCollectionBuilder = new PojoIndexingProcessorValueNodeBuilderDelegate<>(
				modelPath,
				mappingHelper, bindingContext
		);
	}

	public PojoMappingCollectorValueNode value() {
		return valueNodeProcessorCollectionBuilder;
	}

	@Override
	BoundPojoModelPathValueNode<?, ? extends C, V> getModelPath() {
		return modelPath;
	}

	@Override
	void closeOnFailure() {
		valueNodeProcessorCollectionBuilder.closeOnFailure();
	}

	Optional<PojoIndexingProcessorContainerElementNode<C, V>> build(
			PojoIndexingDependencyCollectorPropertyNode<?, P> parentDependencyCollector) {
		try {
			return doBuild( parentDependencyCollector );
		}
		catch (RuntimeException e) {
			getFailureCollector().add( e );
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
			 * it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoIndexingProcessorContainerElementNode<>(
					extractor, immutableNestedProcessors
			) );
		}
	}
}
