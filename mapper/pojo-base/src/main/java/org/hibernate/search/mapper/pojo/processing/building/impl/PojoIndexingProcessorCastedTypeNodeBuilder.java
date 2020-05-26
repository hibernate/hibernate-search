/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.Collection;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorValueNode;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathCastedTypeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorCastedTypeNode;

/**
 * A builder of {@link PojoIndexingProcessorCastedTypeNode}.
 *
 * @param <T> The processed type received as input.
 * @param <U> The type the input objects will be casted to.
 */
public class PojoIndexingProcessorCastedTypeNodeBuilder<T, U> extends AbstractPojoIndexingProcessorTypeNodeBuilder<T, U> {

	private final BoundPojoModelPathCastedTypeNode<T, U> modelPath;

	public PojoIndexingProcessorCastedTypeNodeBuilder(
			BoundPojoModelPathCastedTypeNode<T, U> modelPath,
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext,
			Optional<PojoIdentityMappingCollector> identityMappingCollector,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences) {
		super( mappingHelper, bindingContext, identityMappingCollector, parentIndexObjectReferences );
		this.modelPath = modelPath;
	}

	@Override
	public BoundPojoModelPathCastedTypeNode<T, U> getModelPath() {
		return modelPath;
	}

	@Override
	protected PojoIndexingDependencyCollectorTypeNode<U> toType(
			PojoIndexingDependencyCollectorValueNode<?, T> valueDependencyCollector) {
		return valueDependencyCollector.castedType( getModelPath().getTypeModel() );
	}

	@Override
	protected PojoIndexingProcessor<T> doBuild(Collection<IndexObjectFieldReference> parentIndexObjectReferences,
			Collection<BeanHolder<? extends TypeBridge>> immutableBridgeHolders,
			PojoIndexingProcessor<? super U> nested) {
		return new PojoIndexingProcessorCastedTypeNode<>(
				getModelPath().getTypeModel().caster(),
				parentIndexObjectReferences, immutableBridgeHolders, nested
		);
	}
}
