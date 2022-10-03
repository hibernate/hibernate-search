/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.building.impl;

import java.util.Collection;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorMultiNode;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

abstract class AbstractPojoProcessorNodeBuilder {

	final PojoMappingHelper mappingHelper;
	final IndexBindingContext bindingContext;

	AbstractPojoProcessorNodeBuilder(
			PojoMappingHelper mappingHelper, IndexBindingContext bindingContext) {
		this.mappingHelper = mappingHelper;
		this.bindingContext = bindingContext;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getModelPath() + "]";
	}

	abstract BoundPojoModelPath getModelPath();

	abstract void closeOnFailure();

	public final ContextualFailureCollector failureCollector() {
		BoundPojoModelPath modelPath = getModelPath();

		ContextualFailureCollector failureCollector = mappingHelper.failureCollector()
				.withContext(
						PojoEventContexts.fromType( modelPath.getRootType().rawType() )
				);

		PojoModelPath unboundPath = modelPath.toUnboundPath();
		if ( unboundPath != null ) {
			failureCollector = failureCollector.withContext(
					PojoEventContexts.fromPath( modelPath.toUnboundPath() )
			);
		}

		return failureCollector;
	}

	protected final <T> PojoIndexingProcessor<? super T> createNested(
			Collection<? extends PojoIndexingProcessor<? super T>> elements) {
		int size = elements.size();
		if ( size == 0 ) {
			// Simplify the tree: no need for a node here
			return PojoIndexingProcessor.noOp();
		}
		else if ( size == 1 ) {
			// Simplify the tree: no need for a multi-node here
			return elements.iterator().next();
		}
		else {
			return new PojoIndexingProcessorMultiNode<>( elements );
		}
	}

}
