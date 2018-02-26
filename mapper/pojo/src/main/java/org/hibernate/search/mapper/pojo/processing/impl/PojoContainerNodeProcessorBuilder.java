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

import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractor;

/**
 * @author Yoann Rodiere
 */
public class PojoContainerNodeProcessorBuilder<C, T> extends AbstractPojoNodeProcessorBuilder<C> {

	private final PojoTypeModel<T> elementTypeModel;
	private final ContainerValueExtractor<C, T> extractor;

	private final Collection<FunctionBridgeProcessor<? super T, ?>> bridgeProcessors = new ArrayList<>();

	private final Collection<AbstractPojoNodeProcessorBuilder<? super T>> elementProcessorBuilders = new ArrayList<>();

	PojoContainerNodeProcessorBuilder(
			PojoPropertyNodeProcessorBuilder<?, ? extends C> parent,
			PojoTypeModel<T> elementTypeModel, ContainerValueExtractor<C, T> extractor,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext) {
		super( parent, contributorProvider, indexModelBinder, bindingContext );
		this.elementTypeModel = elementTypeModel;
		this.extractor = extractor;
	}

	public void functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder, String defaultedFieldName,
			FieldModelContributor fieldModelContributor) {
		indexModelBinder.addFunctionBridge(
				bindingContext, elementTypeModel, builder, defaultedFieldName,
				fieldModelContributor
		)
				.ifPresent( bridgeProcessors::add );
	}

	public void indexedEmbedded(IndexModelBindingContext nestedBindingContext) {
		PojoTypeNodeProcessorBuilder<T> nestedProcessorBuilder = new PojoTypeNodeProcessorBuilder<>(
				this, elementTypeModel, contributorProvider, indexModelBinder, nestedBindingContext,
				PojoTypeNodeIdentityMappingCollector.noOp() // Do NOT propagate the identity mapping collector to IndexedEmbeddeds
		);
		elementProcessorBuilders.add( nestedProcessorBuilder );
		contributorProvider.get( elementTypeModel.getRawType() )
				.forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "[" ).append( extractor ).append( "]" );
	}

	@Override
	Optional<PojoContainerNodeProcessor<C, T>> build() {
		Collection<PojoNodeProcessor<? super T>> immutableNestedProcessors =
				bridgeProcessors.isEmpty() && elementProcessorBuilders.isEmpty()
						? Collections.emptyList()
						: new ArrayList<>( bridgeProcessors.size() + elementProcessorBuilders.size() );
		immutableNestedProcessors.addAll( bridgeProcessors );
		elementProcessorBuilders.stream()
				.map( AbstractPojoNodeProcessorBuilder::build )
				.filter( Optional::isPresent )
				.map( Optional::get )
				.forEach( immutableNestedProcessors::add );

		if ( immutableNestedProcessors.isEmpty() ) {
			/*
			 * If this processor doesn't have any bridge, nor any nested processor,
			 * it is useless and we don't need to build it
			 */
			return Optional.empty();
		}
		else {
			return Optional.of( new PojoContainerNodeProcessor<>(
					extractor, immutableNestedProcessors
			) );
		}
	}
}
