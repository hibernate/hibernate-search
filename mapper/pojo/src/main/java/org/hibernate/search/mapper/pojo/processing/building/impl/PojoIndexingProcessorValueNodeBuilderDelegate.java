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

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingHelper;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorValueBridgeNode;

public class PojoIndexingProcessorValueNodeBuilderDelegate<V> implements PojoMappingCollectorValueNode {

	private final BoundPojoModelPathValueNode<?, ?, V> modelPath;

	private final PojoMappingHelper mappingHelper;
	private final IndexModelBindingContext bindingContext;

	private final Collection<PojoIndexingProcessorValueBridgeNode<? super V, ?>> bridgeNodes = new ArrayList<>();

	private final Collection<PojoIndexingProcessorTypeNodeBuilder<? super V>> typeNodeBuilders = new ArrayList<>();

	PojoIndexingProcessorValueNodeBuilderDelegate(
			BoundPojoModelPathValueNode<?, ?, V> modelPath,
			PojoMappingHelper mappingHelper, IndexModelBindingContext bindingContext) {
		this.modelPath = modelPath;
		this.mappingHelper = mappingHelper;
		this.bindingContext = bindingContext;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + modelPath + "]";
	}

	@Override
	public void valueBridge(BridgeBuilder<? extends ValueBridge<?, ?>> builder, String fieldName,
			FieldModelContributor fieldModelContributor) {
		String defaultedFieldName = fieldName;
		if ( defaultedFieldName == null ) {
			defaultedFieldName = modelPath.parent().getPropertyHandle().getName();
		}

		mappingHelper.getIndexModelBinder().addValueBridge(
				bindingContext, modelPath.type().getTypeModel(), builder, defaultedFieldName,
				fieldModelContributor
		)
				.ifPresent( bridgeNodes::add );
	}

	@Override
	public void indexedEmbedded(String relativePrefix, ObjectFieldStorage storage,
			Integer maxDepth, Set<String> includePaths) {
		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = modelPath.parent().getPropertyHandle().getName() + ".";
		}

		BoundPojoModelPathTypeNode<?> holderTypePath = modelPath.parent().parent();

		Optional<IndexModelBindingContext> nestedBindingContextOptional = bindingContext.addIndexedEmbeddedIfIncluded(
				holderTypePath.getTypeModel().getRawType(),
				defaultedRelativePrefix, storage, maxDepth, includePaths
		);
		nestedBindingContextOptional.ifPresent( nestedBindingContext -> {
			BoundPojoModelPathTypeNode<V> embeddedTypeModelPath = modelPath.type();
			PojoIndexingProcessorTypeNodeBuilder<V> nestedProcessorBuilder = new PojoIndexingProcessorTypeNodeBuilder<>(
					embeddedTypeModelPath, mappingHelper, nestedBindingContext,
					// Do NOT propagate the identity mapping collector to IndexedEmbeddeds
					PojoIdentityMappingCollector.noOp()
			);
			typeNodeBuilders.add( nestedProcessorBuilder );
			mappingHelper.getContributorProvider().forEach(
					embeddedTypeModelPath.getTypeModel().getRawType(),
					c -> c.contributeMapping( nestedProcessorBuilder )
			);
		} );
	}

	Collection<PojoIndexingProcessor<? super V>> build() {
		Collection<PojoIndexingProcessor<? super V>> immutableNestedNodes =
				bridgeNodes.isEmpty() && typeNodeBuilders.isEmpty()
						? Collections.emptyList()
						: new ArrayList<>( bridgeNodes.size() + typeNodeBuilders.size() );
		immutableNestedNodes.addAll( bridgeNodes );
		typeNodeBuilders.stream()
				.map( AbstractPojoProcessorNodeBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedNodes::add );

		return immutableNestedNodes;
	}
}
