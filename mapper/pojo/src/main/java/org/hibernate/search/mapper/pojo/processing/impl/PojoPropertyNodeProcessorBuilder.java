/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoIndexModelBinder;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeIdentityMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
public class PojoPropertyNodeProcessorBuilder extends AbstractPojoProcessorBuilder
		implements PojoPropertyNodeMappingCollector {

	private final PropertyHandle propertyHandle;

	private final Collection<PojoTypeNodeProcessorBuilder> indexedEmbeddedProcessorBuilders = new ArrayList<>();

	public PojoPropertyNodeProcessorBuilder(
			PojoTypeNodeProcessorBuilder parent, PropertyHandle propertyHandle, PojoTypeModel<?> propertyTypeModel,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider,
			PojoIndexModelBinder indexModelBinder, IndexModelBindingContext bindingContext,
			PojoTypeNodeIdentityMappingCollector identityMappingCollector) {
		super( parent, propertyTypeModel,
				contributorProvider, indexModelBinder, bindingContext, identityMappingCollector );
		this.propertyHandle = propertyHandle;
	}

	@Override
	public void functionBridge(BridgeBuilder<? extends FunctionBridge<?, ?>> builder,
			String fieldName, FieldModelContributor fieldModelContributor) {
		String defaultedFieldName = fieldName;
		if ( defaultedFieldName == null ) {
			defaultedFieldName = propertyHandle.getName();
		}

		ValueProcessor processor = indexModelBinder.addFunctionBridge(
				bindingContext, indexableModel, typeModel, builder, defaultedFieldName, fieldModelContributor );
		processors.add( processor );
	}

	@Override
	@SuppressWarnings( {"rawtypes", "unchecked"} )
	public void identifierBridge(BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		createAndCollectIdentifierBridge( typeModel, propertyHandle, builder );
	}

	@Override
	public void containedIn() {
		// FIXME implement ContainedIn
		// FIXME also contribute containedIns to indexedEmbeddeds using the parent's metadata here, if possible?
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void indexedEmbedded(String relativePrefix, ObjectFieldStorage storage,
			Integer maxDepth, Set<String> includePaths) {
		// TODO handle collections

		String defaultedRelativePrefix = relativePrefix;
		if ( defaultedRelativePrefix == null ) {
			defaultedRelativePrefix = propertyHandle.getName() + ".";
		}

		Optional<IndexModelBindingContext> nestedBindingContextOptional = bindingContext.addIndexedEmbeddedIfIncluded(
				parent.typeModel, defaultedRelativePrefix, storage, maxDepth, includePaths );
		nestedBindingContextOptional.ifPresent( nestedBindingContext -> {
			PojoTypeNodeProcessorBuilder nestedProcessorBuilder = new PojoTypeNodeProcessorBuilder(
					this, indexableModel.getTypeModel(), contributorProvider, indexModelBinder, nestedBindingContext,
					PojoTypeNodeIdentityMappingCollector.noOp() // Do NOT propagate the identity mapping collector to IndexedEmbeddeds
			);
			indexedEmbeddedProcessorBuilders.add( nestedProcessorBuilder );
			contributorProvider.get( typeModel ).forEach( c -> c.contributeMapping( nestedProcessorBuilder ) );
		} );
	}

	@Override
	protected void appendSelfPath(StringBuilder builder) {
		builder.append( "." ).append( propertyHandle.getName() );
	}

	private <T> void createAndCollectIdentifierBridge(PojoTypeModel<T> typeModel, PropertyHandle propertyHandle,
			BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		IdentifierBridge<T> bridge = indexModelBinder.createIdentifierBridge( indexableModel, typeModel, builder );
		identityMappingCollector.identifierBridge( typeModel, propertyHandle, bridge );
	}

	public PojoPropertyNodeProcessor build() {
		return new PojoPropertyNodeProcessor( propertyHandle, processors, indexedEmbeddedProcessorBuilders );
	}

}
