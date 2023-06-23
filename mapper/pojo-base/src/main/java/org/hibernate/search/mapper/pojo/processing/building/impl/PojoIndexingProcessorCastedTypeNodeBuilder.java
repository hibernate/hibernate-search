/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.AbstractPojoIndexingDependencyCollectorDirectValueNode;
import org.hibernate.search.mapper.pojo.automaticindexing.building.impl.PojoIndexingDependencyCollectorTypeNode;
import org.hibernate.search.mapper.pojo.identity.impl.PojoIdentityMappingCollector;
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
			PojoIdentityMappingCollector identityMappingCollector,
			Collection<IndexObjectFieldReference> parentIndexObjectReferences) {
		super( mappingHelper, bindingContext, identityMappingCollector, parentIndexObjectReferences );
		this.modelPath = modelPath;
	}

	@Override
	public BoundPojoModelPathCastedTypeNode<T, U> getModelPath() {
		return modelPath;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected PojoIndexingDependencyCollectorTypeNode<U> toType(
			AbstractPojoIndexingDependencyCollectorDirectValueNode<?, T> valueDependencyCollector) {
		// By construction, the casted type should be the same as U
		return (PojoIndexingDependencyCollectorTypeNode<U>) valueDependencyCollector
				.castedType( getModelPath().getTypeModel().rawType() );
	}

	@Override
	protected PojoIndexingProcessor<T> doBuild(Collection<IndexObjectFieldReference> parentIndexObjectReferences,
			PojoIndexingProcessor<? super U> nested) {
		return new PojoIndexingProcessorCastedTypeNode<>(
				getModelPath().getTypeModel().rawType().caster(),
				parentIndexObjectReferences, nested,
				typeAdditionalMetadata().isEntity()
		);
	}
}
