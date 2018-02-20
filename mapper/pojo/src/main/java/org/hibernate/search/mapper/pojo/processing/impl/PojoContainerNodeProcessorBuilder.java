/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoContainerTypeModel;
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
			PojoPropertyNodeProcessorBuilder<?, ? extends C> parent, PojoContainerTypeModel<T> containerModel,
			ContainerValueExtractor<C, T> extractor,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext) {
		this(
				parent, containerModel.getElementTypeModel(), extractor,
				contributorProvider, indexModelBinder, bindingContext
		);
	}

	private PojoContainerNodeProcessorBuilder(
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
		FunctionBridgeProcessor<T, ?> processor = indexModelBinder.addFunctionBridge(
				bindingContext, elementTypeModel, builder, defaultedFieldName,
				fieldModelContributor
		);
		bridgeProcessors.add( processor );
	}

	public void indexedEmbedded(IndexModelBindingContext nestedBindingContext) {
		PojoTypeNodeProcessorBuilder<T> nestedProcessorBuilder = new PojoTypeNodeProcessorBuilder<>(
				this, elementTypeModel, contributorProvider, indexModelBinder, nestedBindingContext,
				PojoTypeNodeIdentityMappingCollector.noOp() // Do NOT propagate the identity mapping collector to IndexedEmbeddeds
		);
		elementProcessorBuilders.add( nestedProcessorBuilder );
		contributorProvider.get( elementTypeModel ).forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "[" ).append( extractor ).append( "]" );
	}

	@Override
	PojoContainerNodeProcessor<C, T> build() {
		return new PojoContainerNodeProcessor<>( extractor, bridgeProcessors, elementProcessorBuilders );
	}
}
