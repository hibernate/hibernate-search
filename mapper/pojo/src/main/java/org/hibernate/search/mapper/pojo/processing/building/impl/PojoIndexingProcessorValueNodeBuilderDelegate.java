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
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMappingCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessorFunctionBridgeNode;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;

public class PojoIndexingProcessorValueNodeBuilderDelegate<T> implements PojoMappingCollectorValueNode {

	private final AbstractPojoProcessorNodeBuilder<?> parentBuilder;

	private final PojoIndexModelBinder indexModelBinder;
	private final TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider;
	private final IndexModelBindingContext bindingContext;

	private final PojoTypeModel<?> parentTypeModel;
	private final String defaultName;
	private final PojoTypeModel<T> valueTypeModel;

	private final Collection<PojoIndexingProcessorFunctionBridgeNode<? super T, ?>> bridgeNodes = new ArrayList<>();

	private final Collection<PojoIndexingProcessorTypeNodeBuilder<? super T>> typeNodeBuilders = new ArrayList<>();

	PojoIndexingProcessorValueNodeBuilderDelegate(AbstractPojoProcessorNodeBuilder<?> parentBuilder,
			PojoTypeModel<?> parentTypeModel, String defaultName,
			PojoTypeModel<T> valueTypeModel,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext) {
		this.parentBuilder = parentBuilder;
		this.indexModelBinder = indexModelBinder;
		this.contributorProvider = contributorProvider;
		this.bindingContext = bindingContext;

		this.parentTypeModel = parentTypeModel;
		this.defaultName = defaultName;
		this.valueTypeModel = valueTypeModel;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + parentBuilder.toString() + "]";
	}

	@Override
	public void functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder, String fieldName,
			FieldModelContributor fieldModelContributor) {
		String defaultedFieldName = fieldName;
		if ( defaultedFieldName == null ) {
			defaultedFieldName = defaultName;
		}

		indexModelBinder.addFunctionBridge(
				bindingContext, valueTypeModel, builder, defaultedFieldName,
				fieldModelContributor
		)
				.ifPresent( bridgeNodes::add );
	}

	@Override
	public void indexedEmbedded(String relativePrefix, ObjectFieldStorage storage,
			Integer maxDepth, Set<String> includePaths) {
		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = defaultName + ".";
		}

		Optional<IndexModelBindingContext> nestedBindingContextOptional = bindingContext.addIndexedEmbeddedIfIncluded(
				parentTypeModel.getRawType(), defaultedRelativePrefix, storage, maxDepth, includePaths );
		nestedBindingContextOptional.ifPresent( nestedBindingContext -> {
			PojoIndexingProcessorTypeNodeBuilder<T> nestedProcessorBuilder = new PojoIndexingProcessorTypeNodeBuilder<>(
					parentBuilder, valueTypeModel, contributorProvider, indexModelBinder, nestedBindingContext,
					// Do NOT propagate the identity mapping collector to IndexedEmbeddeds
					PojoIdentityMappingCollector.noOp()
			);
			typeNodeBuilders.add( nestedProcessorBuilder );
			contributorProvider.forEach(
					valueTypeModel.getRawType(),
					c -> c.contributeMapping( nestedProcessorBuilder )
			);
		} );
	}

	Collection<PojoIndexingProcessor<? super T>> build() {
		Collection<PojoIndexingProcessor<? super T>> immutableNestedNodes =
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
