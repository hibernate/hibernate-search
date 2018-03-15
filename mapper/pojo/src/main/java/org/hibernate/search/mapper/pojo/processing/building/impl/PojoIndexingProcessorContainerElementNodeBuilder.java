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
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorContainerElementNode;

public class PojoIndexingProcessorContainerElementNodeBuilder<C, V> extends AbstractPojoProcessorNodeBuilder<C> {

	private final BoundPojoModelPathValueNode<?, ? extends C, V> modelPath;
	private final ContainerValueExtractor<C, V> extractor;

	private final PojoIndexingProcessorValueNodeBuilderDelegate<V> valueNodeProcessorCollectionBuilder;

	PojoIndexingProcessorContainerElementNodeBuilder(BoundPojoModelPathValueNode<?, ? extends C, V> modelPath,
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
	Optional<PojoIndexingProcessorContainerElementNode<C, V>> build() {
		Collection<PojoIndexingProcessor<? super V>> immutableNestedProcessors =
				valueNodeProcessorCollectionBuilder.build();

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
