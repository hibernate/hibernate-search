/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

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
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoValueNodeMappingCollector;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
public class PojoValueNodeProcessorCollectionBuilder<T> implements PojoValueNodeMappingCollector {

	private final AbstractPojoNodeProcessorBuilder<?> parentBuilder;

	private final PojoIndexModelBinder indexModelBinder;
	private final TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider;
	private final IndexModelBindingContext bindingContext;

	private final PojoTypeModel<?> parentTypeModel;
	private final String defaultName;
	private final PojoTypeModel<T> valueTypeModel;

	private final Collection<FunctionBridgeProcessor<? super T, ?>> bridgeProcessors = new ArrayList<>();

	private final Collection<AbstractPojoNodeProcessorBuilder<? super T>> valueProcessorBuilders = new ArrayList<>();

	PojoValueNodeProcessorCollectionBuilder(AbstractPojoNodeProcessorBuilder<?> parentBuilder,
			PojoTypeModel<?> parentTypeModel, String defaultName,
			PojoTypeModel<T> valueTypeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
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
				.ifPresent( bridgeProcessors::add );
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
			PojoTypeNodeProcessorBuilder<T> nestedProcessorBuilder = new PojoTypeNodeProcessorBuilder<>(
					parentBuilder, valueTypeModel, contributorProvider, indexModelBinder, nestedBindingContext,
					// Do NOT propagate the identity mapping collector to IndexedEmbeddeds
					PojoTypeNodeIdentityMappingCollector.noOp()
			);
			valueProcessorBuilders.add( nestedProcessorBuilder );
			contributorProvider.get( valueTypeModel.getRawType() )
					.forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
		} );
	}

	Collection<PojoNodeProcessor<? super T>> build() {
		Collection<PojoNodeProcessor<? super T>> immutableNestedProcessors =
				bridgeProcessors.isEmpty() && valueProcessorBuilders.isEmpty()
						? Collections.emptyList()
						: new ArrayList<>( bridgeProcessors.size() + valueProcessorBuilders.size() );
		immutableNestedProcessors.addAll( bridgeProcessors );
		valueProcessorBuilders.stream()
				.map( AbstractPojoNodeProcessorBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedProcessors::add );

		return immutableNestedProcessors;
	}
}
